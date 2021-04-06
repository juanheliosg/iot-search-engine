package v1.extractor.actors

import akka.Done
import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}
import akka.pattern.StatusReply
import akka.stream.KillSwitch
import play.api.Configuration
import v1.extractor.actors.ExtractorGuardianEntity.extractorIsRunning
import v1.extractor.http.HTTPExtractor
import v1.extractor.{ExtractorState, ExtractorType}

import java.time.LocalDateTime

object Extractor{
  sealed trait Command extends CborSerializable


  /**
   * Command send to shutdown the actor in a synchronous and graceful way.
   * @param replyTo
   */
  final case class GracefulShutdown(replyTo: ActorRef[StatusReply[Done]]) extends Command
  /**
   * Command send by the stream for communicating errors to the Extractor
   * @param msg
   */
  final case class StreamError(msg: String) extends Command

  /**
   * Command send by the supervised stream to confirm that stream is running
   */
  final case object StreamRunning extends Command

  def apply(extData: ExtractorState, parent: ActorRef[ExtractorGuardianEntity.Command], playActorConfig: PlayActorConfig): Behavior[Command] =
    Behaviors.setup(context => new Extractor(context,parent, extData, playActorConfig))
}

/**
 * Play! and akka classes needed for akka actos and streams
 * @param actorSystem
 * @param config
 */
class PlayActorConfig (val actorSystem: ActorSystem, val config: Configuration)

/**
 * Akka actor encapsulating an akka stream for the given extData type.
 * @param context ActorContext  for akka stream
 * @param extData data for the akka operation
 * @param playActorConfig generic configuration for the stream
 */
class Extractor (context: ActorContext[Extractor.Command], parent: ActorRef[ExtractorGuardianEntity.Command], extData: ExtractorState, playActorConfig: PlayActorConfig)
  extends AbstractBehavior[Extractor.Command] (context){
  import Extractor._

  val killSwitch: KillSwitch = extData.extractorType match{
    case  ExtractorType.Http => HTTPExtractor.extraction(extData, context.self, playActorConfig)
    case _ => throw new IllegalStateException()
  }

  override def onMessage(cmd: Command): Behavior[Command] = {
    cmd match {
      case StreamError(msg) =>
        context.log
          .error(s"${java.time.LocalDateTime.now()}  ${extData.extractorType} extractor with parent ${parent.path} failed: \n $msg")
        killSwitch.shutdown()
        throw new RuntimeException(msg);
      case `StreamRunning` =>
        parent ! extractorIsRunning
        Behaviors.same
      case GracefulShutdown(replyTo) =>
        replyTo ! StatusReply.Ack
        Behaviors.stopped
    }

  }

  override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
    case PostStop =>
      context.log.info(s"Extractor for parent ${parent.path} stopped")
      Behaviors.same
  }
}