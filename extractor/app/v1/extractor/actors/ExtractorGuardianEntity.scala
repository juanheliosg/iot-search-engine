package v1.extractor.actors
import akka.Done
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityTypeKey}
import akka.pattern.StatusReply
import akka.persistence.typed.{PersistenceId, RecoveryCompleted}
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}
import akka.util.Timeout
import v1.extractor.ExtractorState

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}



/**
 * Persistence actor who stores the extractor state.
 * Is father of the real extractor actor.
 */
object ExtractorGuardianEntity{
  sealed trait Command extends CborSerializable

  /**
   * Command for updating the extractor state.
   * Change the extractor guardian state to Starting if successful
   * @param extractor extData
   * @param replyTo reply ref
   */
  final case class updateExtractor(extractor: ExtractorState
                                   ,replyTo: ActorRef[StatusReply[Summary]]) extends Command

  /**
   * Get all the extractor data
   * @param replyTo reply with the extractor data
   */
  final case class getExtractor(replyTo: ActorRef[StatusReply[Summary]]) extends Command

  /**
   * Get the extractor status
   * @param replyTo actor ref
   */
  final case class getStatus(replyTo: ActorRef[StatusReply[Status]]) extends Command

  /**
   * Start the extractor if it is stopped
   * @param replyTo actor ref
   */
  final case class startExtractor(replyTo: ActorRef[StatusReply[Done]]) extends Command

  /**
   * Stop the extractor if it is running or starting
   * @param replyTo actor ref
   */
  final case class stopExtractor(replyTo: ActorRef[StatusReply[Done]]) extends Command

  /**
   * Command for passivating the actor removing it from the actor system permanently
   */
  final case object ExterminateExtractor extends Command
  /**
   * Message send when the stream terminates
   */
  final case object failureOnExtractor extends Command

  /**
   * Message send by the stream if is correctly running
   */
  final case object extractorIsRunning extends Command

  /**
   * Message send when child is killed correctly so a new child can be created
   */
  final case object KilledChildStartNewOne extends Command

  /**
   * Summary of the state of the extractor
   * @param extractorState extractor state
   * @param status id and status of ext
   */
  final case class Summary(extractorState: ExtractorState, status: Status) extends CborSerializable

  /**
   * Encapsulate guardian status
   * @param id entity id
   * @param status status
   */
  final case class Status(id: String,status: String) extends CborSerializable

  sealed trait Event extends CborSerializable

  /**
   * Event fired when extractorUpdate command is accepted
   * @param extractorState state
   */
  case class extractorUpdated(extractorState: ExtractorState) extends Event

  /**
   * Event fired when starting command is accpeted
   */
  case object ExtractorBeganStarting extends Event

  /**
   * Event fired when stopped command is accepted
   */
  case object ExtractorStopped extends Event

  /**
   * Event fired when running command is accepted
   */
  case object ExtractorStarted extends Event

  /**
   * Event fired when extractor has failed.
   */
  case object ExtractorFailed extends Event



  type ReplyEffect = akka.persistence.typed.scaladsl.ReplyEffect[Event, ExtractorGuardian]
  implicit val timeout: Timeout = 3.seconds


  sealed trait ExtractorGuardian extends CborSerializable{
    def applyCommand(cmd: Command, context: ActorContext[Command], playActorConfig: PlayActorConfig): ReplyEffect
    def applyEvent(event: Event, context: ActorContext[Command], playActorConfig: PlayActorConfig): ExtractorGuardian
  }


  private def startExtractorChild(extData: ExtractorState,
                                  context: ActorContext[Command],
                                  playActorConfig: PlayActorConfig): Unit = {

    if (context.child("extractor").nonEmpty) {
      val child: ActorRef[Extractor.Command] = context.child("extractor").get.asInstanceOf[ActorRef[Extractor.Command]]
      context.askWithStatus(child,ref => Extractor.GracefulShutdown(ref)){
        case Success(message) =>
          context.log.info(s"Child killed for parent ${context.self.path}")
          KilledChildStartNewOne
        case Failure(exception) =>
          exception.printStackTrace()
          KilledChildStartNewOne //Normally failure is due to a timeout so we restart
      }
    }
    else{
      context.log.info(s"Starting child for ${context.self.path} actor")
      val child = context.spawn(Extractor(extData, context.self, playActorConfig),"extractor")
      context.watchWith(child, failureOnExtractor)
    }
  }

  private def updateAndStartExtractorEffect(extData: ExtractorState, entityID: String, state: String,
                                            replyTo: ActorRef[StatusReply[Summary]],
                                            context: ActorContext[Command],playActorConfig: PlayActorConfig ) = {

    Effect.persist(extractorUpdated(extData))
      .thenRun( (newState :ExtractorGuardian) =>  startExtractorChild(extData, context, playActorConfig))
      .thenReply(replyTo)(_ => StatusReply.Success(Summary(extData,Status(entityID, state))))

  }

  private def updateExtractorEffect(extData: ExtractorState, replyTo: ActorRef[StatusReply[Summary]], entityID: String, state: String): ReplyEffect  = {
    Effect.persist(extractorUpdated(extData))
      .thenReply(replyTo)(_ => StatusReply.Success(Summary(extData,Status(entityID,state))))
  }

  /**
   * Initial state for the extractor guardian. Starts with an empty ext state.
   * This state is equivalent to not "existing" for the requests, as a running actor always have to be started.
   * No extraction actor is running in this state
   * @param extractorState optional extractor state.
   */
  case class NotStartedExtractor(entityID: String ,extractorState: Option[ExtractorState] = None) extends ExtractorGuardian{
    val state = "not started"
    override def applyCommand(cmd: Command, context: ActorContext[Command], playActorConfig: PlayActorConfig): ReplyEffect = {
      cmd  match {
        case updateExtractor(extData, replyTo) =>
          updateExtractorEffect(extData, replyTo, entityID, state)
        case startExtractor(replyTo) =>
          if (extractorState.nonEmpty){
            Effect.persist(ExtractorBeganStarting)
              .thenRun( (newState :ExtractorGuardian) =>  startExtractorChild(extractorState.get, context, playActorConfig))
              .thenReply(replyTo)(_ => StatusReply.Ack)
          }
          else{
            Effect.reply(replyTo)(StatusReply.Error("Extractor state is empty. Update state for starting the extractor"))
          }
        case getExtractor(replyTo) =>
          if (extractorState.nonEmpty) {
            Effect.reply(replyTo)(
              StatusReply.Success(Summary(extractorState.get, Status(entityID,state))))
          }
          else {
            Effect.reply(replyTo)(StatusReply.Error("Extractor state is empty"))
          }
        case getStatus(replyTo) => Effect.reply(replyTo)(StatusReply.Success(Status(entityID,state)))
        case _ => Effect.unhandled.thenNoReply()
      }
    }
    override def applyEvent(event: Event, context: ActorContext[Command], playActorConfig: PlayActorConfig): ExtractorGuardian = {
      event match{
        case extractorUpdated(extData) => NotStartedExtractor(entityID,Some(extData))
        case ExtractorBeganStarting => StartingExtractor(entityID, extractorState.get)
        case _ => throw new IllegalStateException(s"Unexpected event [$event] in state NotStartedExtractor")
      }
    }
  }

  /**
   * Starting state, the extractor stream is running but not initialization has not been completed,
   * pass to running state if instantiation has been successfully completed
   * Update message restart the actor
   * @param extractorState extractor data
   */
  case class StartingExtractor(entityID: String, extractorState: ExtractorState, isUpdating :Boolean = false)
    extends ExtractorGuardian{
    val state = "starting"
    override def applyCommand(cmd: Command, context: ActorContext[Command], playActorConfig: PlayActorConfig): ReplyEffect = {
      cmd match {
        case updateExtractor(extData, replyTo) =>
          updateAndStartExtractorEffect(extData, entityID, state, replyTo, context, playActorConfig)
        case stopExtractor(replyTo) =>
          if (context.child("extractor").nonEmpty)
            context.stop(context.child("extractor").get)
          Effect.persist(ExtractorStopped).thenReply(replyTo)(_ => StatusReply.Ack)
        case `failureOnExtractor` =>
          //nunca hay un failure realmente cuan
          if (isUpdating)
            Effect.noReply
          else
            Effect.persist(ExtractorFailed).thenNoReply()
        case `extractorIsRunning` =>
          Effect.persist(ExtractorStarted).thenNoReply()
         case getExtractor(replyTo) =>
          Effect.reply(replyTo)(StatusReply.Success(Summary(extractorState,Status(entityID,state))))
        case getStatus(replyTo) =>
          Effect.reply(replyTo)(StatusReply.Success(Status(entityID,state)))
        case _ => Effect.unhandled.thenNoReply()
      }
    }
    override def applyEvent(event: Event, context: ActorContext[Command], playActorConfig: PlayActorConfig): ExtractorGuardian = {
      event match{
        case extractorUpdated(extData) => StoppedExtractor(entityID, extData)
        case ExtractorStopped => StoppedExtractor(entityID,extractorState)
        case ExtractorFailed => FailedExtractor(entityID,extractorState)
        case ExtractorStarted => RunningExtractor(entityID, extractorState)
        case _ => throw new IllegalStateException(s"Unexpected event [$event] in state StartingExtractor")
      }
    }
  }

  /**
   * Running state representing that extractor actor is working and sending data
   * Manage extractor actor error passing to error state
   * Update message restart the actor
   * @param extractorState extractor data
   */
  case class RunningExtractor(entityID: String ,extractorState: ExtractorState) extends ExtractorGuardian{
    val state = "running"

    override def applyCommand(cmd: Command, context: ActorContext[Command], playActorConfig: PlayActorConfig): ReplyEffect = {
      cmd match {
        case updateExtractor(extData, replyTo) =>
          updateAndStartExtractorEffect(extData, entityID, state, replyTo, context, playActorConfig)
        case stopExtractor(replyTo) =>
          context.child("extractor") match {
            case Some(child) => context.stop(child)
          }
          Effect.persist(ExtractorStopped).thenReply(replyTo)(_ => StatusReply.Ack)
        case `failureOnExtractor` =>
          Effect.persist(ExtractorFailed).thenNoReply()
        case getExtractor(replyTo) => Effect.reply(replyTo)(
          StatusReply.Success(Summary(extractorState,Status(entityID, state))))
        case getStatus(replyTo) => Effect.reply(replyTo)(StatusReply.Success(Status(entityID,state)))
        case _ => Effect.unhandled.thenNoReply()
      }
    }
    override def applyEvent(event: Event, context: ActorContext[Command], playActorConfig: PlayActorConfig): ExtractorGuardian = {
      event match{
        case extractorUpdated(extData) => StoppedExtractor(entityID, extData)
        case ExtractorStopped => StoppedExtractor(entityID, extractorState)
        case ExtractorFailed=> FailedExtractor(entityID,extractorState)
        case _ => throw new IllegalStateException(s"Unexpected event [$event] in state Running extractor")
      }
    }

  }

  /**
   * Stopped extractor state. In this state there isnt an extractor stream working, is killed or going to be killed soon.
   * Thats why there is a handler for a failure signal
   * @param extractorState extractor data
   */
  case class StoppedExtractor(entityID: String,extractorState: ExtractorState) extends ExtractorGuardian {
    val state = "stopped"

    override def applyCommand(cmd: Command, context: ActorContext[Command], playActorConfig: PlayActorConfig): ReplyEffect = {
      cmd match {
        case updateExtractor(extData, replyTo) =>
          updateExtractorEffect(extData, replyTo, entityID, state)
        case startExtractor(replyTo) =>
          Effect.persist(ExtractorBeganStarting)
            .thenRun( (newState :ExtractorGuardian) =>  startExtractorChild(extractorState, context, playActorConfig))
            .thenReply(replyTo)(_ => StatusReply.Ack)
        case `KilledChildStartNewOne` =>
          Effect.persist(ExtractorBeganStarting).thenRun( (state: ExtractorGuardian) => startExtractorChild(extractorState,context, playActorConfig)).thenNoReply()
        case getExtractor(replyTo) => Effect.reply(replyTo)(StatusReply.Success(
          Summary(extractorState,Status(entityID, state))))
        case getStatus(replyTo) => Effect.reply(replyTo)(StatusReply.Success(Status(entityID, state)))
        case _ => Effect.unhandled.thenNoReply()
      }
    }
    override def applyEvent(event: Event, context: ActorContext[Command], playActorConfig: PlayActorConfig): ExtractorGuardian = {
      event match{
        case extractorUpdated(extData) => StoppedExtractor(entityID,extData)
        case ExtractorBeganStarting => StartingExtractor(entityID, extractorState, isUpdating=true)
        case _ => throw new IllegalStateException(s"Unexpected event [$event] in state StoppedExtractor")
      }
    }
  }

  /**
   * State when the data stream has failed. No running extractor, is the same as actor stopped but an error has occurred previously.
   * Update message restart the actor
   * @param entityID entity id
   * @param extractorState internal ext data
   */
  case class FailedExtractor(entityID: String, extractorState: ExtractorState) extends ExtractorGuardian{
    val state = "error"
    override def applyCommand(cmd: Command, context: ActorContext[Command], playActorConfig: PlayActorConfig): ReplyEffect = {
      cmd match {
        case updateExtractor(extData, replyTo) =>
          updateAndStartExtractorEffect(extData, entityID, state, replyTo, context, playActorConfig)
        case startExtractor(replyTo) =>
          Effect.persist(ExtractorBeganStarting).thenReply(replyTo)(_ => StatusReply.Ack)
        case getExtractor(replyTo) => Effect.reply(replyTo)(StatusReply.Success(
          Summary(extractorState,Status(entityID,state))))
        case getStatus(replyTo) => Effect.reply(replyTo)(StatusReply.Success(Status(entityID,state)))
        case _ => Effect.unhandled.thenNoReply()
      }
    }
    override def applyEvent(event: Event, context: ActorContext[Command], playActorConfig: PlayActorConfig): ExtractorGuardian = {
      event match{
        case extractorUpdated(extData) => StoppedExtractor(entityID, extData)
        case _ => throw new IllegalStateException(s"Unexpected event [$event] in state ErrorState")
      }
    }

  }

  /**
   * Extractor guardian TypeKey
   */
  val TypeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Extractor")

  /**
   * Command parameter
   * @param shard shard for passivating
   * @param state real state
   * @param cmd command sended
   * @param context actor context
   * @param playActorConfig general config including actor system and PlayConfig
   * @return
   */
  def applyCommandCommon( shard: ActorRef[ClusterSharding.ShardCommand],
                          state: ExtractorGuardian, cmd: Command, context: ActorContext[Command],
                          playActorConfig: PlayActorConfig): ReplyEffect = {
    cmd match {
      case `ExterminateExtractor` =>
        Effect.none.thenRun( (newState :ExtractorGuardian) => shard ! ClusterSharding.Passivate(context.self)).thenNoReply()
      case _ => state.applyCommand(cmd, context, playActorConfig)
    }
  }

  def apply(entityID : String, shard: ActorRef[ClusterSharding.ShardCommand] ,persistenceId: PersistenceId, playActorConfig: PlayActorConfig): Behavior[Command] =
    {
      Behaviors.setup{ context =>

        context.log.info("Starting ExtractorGuardian with id {}", entityID)
        EventSourcedBehavior[Command, Event, ExtractorGuardian](
          persistenceId = persistenceId,
          emptyState = NotStartedExtractor(entityID, None),
          commandHandler = (state, cmd) => state.applyCommand(cmd, context, playActorConfig),
          eventHandler = (state, evt) => state.applyEvent(evt, context, playActorConfig))
          .receiveSignal{
            case (state, RecoveryCompleted) =>
              state match {
                case extractor: RunningExtractor =>
                  val runState = extractor.extractorState
                  startExtractorChild(runState, context, playActorConfig)
                case _ =>
              }
              state match {
                case extractor: StartingExtractor =>
                  val runState = extractor.extractorState
                  startExtractorChild(runState, context, playActorConfig)
                case _ =>
              }
          }
          .withRetention(
            RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2))
          .onPersistFailure(
            SupervisorStrategy.restartWithBackoff(200.millis, 5.seconds, 0.1))
      }
    }
}
