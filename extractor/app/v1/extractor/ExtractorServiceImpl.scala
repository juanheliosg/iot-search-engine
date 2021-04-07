package v1.extractor

import akka.actor.ActorSystem
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity}
import akka.pattern.StatusReply
import akka.persistence.cassandra.cleanup.Cleanup
import akka.persistence.typed.PersistenceId
import akka.util.Timeout
import org.slf4j.LoggerFactory
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents, Result}
import v1.extractor.actors.{ExtractorGuardianEntity, PlayActorConfig}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}


/**
 * Implements extractors logic and communicate with Akka actors
 * @param cc controller components
 * @param sharding Cluster Sharding
 * @param config Play! Configuration
 * @param actorSystem Play! Actor system
 */
@Singleton
class ExtractorServiceImpl @Inject() (val cc: ControllerComponents, val sharding: ClusterSharding, val config: Configuration
                                      , val actorSystem: ActorSystem ) extends AbstractController(cc){
  implicit private val timeout: Timeout =
    Timeout.create(java.time.Duration.ofSeconds(config.get[Long]("extractor.timeout-seconds")))
    //Timeout.create(actorSystem.settings.config.getDuration("extractor.timeout"))
  implicit private val executionContext: ExecutionContext = cc.executionContext

  private val logger = LoggerFactory.getLogger(getClass)
  private val playActorConfig = new PlayActorConfig(actorSystem, config)

  /**
   * Sharding region for extractor guardian entities
   */
  sharding.init(
    Entity(typeKey= ExtractorGuardianEntity.TypeKey){
      entityContext => ExtractorGuardianEntity(entityContext.entityId, entityContext.shard,
        PersistenceId.of(ExtractorGuardianEntity.TypeKey.name,entityContext.entityId),
        playActorConfig)}
  )


  /**
   * Transform an ExtractorFormInput into a n extractor state
   * @param extData data to be transformed to state object
   * @return
   */
  private def formToState(extData: ExtractorFormInput): ExtractorState = {
    val extractorType = ExtractorType.withName(extData.extType)
    val inputConfig = extractorType match {
      case ExtractorType.Http => InputConfig(extData.ioConfig.inputConfig.address,
        Some( HttpInputConfig(extData.ioConfig.inputConfig.jsonPath.get,
          extData.ioConfig.inputConfig.freq.get)))
    }

    val IoConfig = IOConfig(inputConfig, extData.ioConfig.kafkaConfig)
    ExtractorState(extData.dataSchema,IoConfig, extractorType)
  }

  /**
   * Get an extractor data using a correct entity id
   * @param id entity id
   * @return extractorGetResponse object containing id, status, type, schema and config
   */
  def getExtractor(id: Long): Future[Result] = {
    val extractorId = id.toString
    val entityRef = sharding.entityRefFor(ExtractorGuardianEntity.TypeKey, extractorId)

    val reply: Future[ExtractorGuardianEntity.Summary] =
      entityRef.askWithStatus(ref => ExtractorGuardianEntity.getExtractor(ref))
    reply.transformWith{
      case Success(summary) => Future(Ok(
        Json.toJson(
        new ExtractorGetResponse(
          extractorId.toLong,
          summary.status.status,
          summary.extractorState.extractorType.toString,
          summary.extractorState.schema,
          summary.extractorState.config
        ))))
      case Failure(StatusReply.ErrorMessage(_)) =>
        //We dont delete the created actor because a post method would be valid
        //i haven't see any examples of deleting the entity if getId is not valid
        Future(BadRequest(JSONError.format(
          Json.obj(
            "id" -> "Extractor with provided id does not exists"
          ))))
      case Failure(_) =>
        Future(BadRequest(JSONError.format(
          Json.obj(
            "general" -> "Failure while getting extractor"
          ))))
      }
  }

  /**
   * Create an akka persistence actor with the extData fails if
   * not started extractor exists with same entity ref
   * @param extData. Validated extractorFormInput
   * @return Final result
   */
  def postExtractor(extData: ExtractorFormInput): Future[Result] = {
    val extractorId = extData.id

    val entityRef = sharding.entityRefFor(ExtractorGuardianEntity.TypeKey, extractorId.toString)
    val reply = entityRef.askWithStatus(ref => ExtractorGuardianEntity.getStatus(ref))

    val result = reply.flatMap(status =>
      if(status.status != "not started"){ //change to enum
        logger.info(s"Request tried to repost extractor with id ${extData.id} ")
        Future(BadRequest(JSONError.format(
          Json.obj(
            "id" -> "Extractor with same ID"
          ))
        ))
      }
      else{
        val newExtractor = sharding.entityRefFor(ExtractorGuardianEntity.TypeKey, extractorId.toString)
        val extState = formToState(extData)
        val reply = newExtractor.askWithStatus(ref => ExtractorGuardianEntity.updateExtractor(extState,ref))
        val result = reply.map( response => {
          logger.info(s"Success creating extractor with $extractorId id")
          newExtractor.ask(ref => ExtractorGuardianEntity.startExtractor(ref))
          Created(
            Json.toJson(new ExtractorStatusResponse(
              response.status.id.toLong, "starting"
            ))
          )
        })
        result
      }
    )
    val finalResult = result.recoverWith{
      case exc =>
        logger.info("Failed to post resource")
        Future(BadRequest(s"Failed to post resource: ${exc.getMessage}"))
    }
    finalResult
  }

  /**
   * Update the extractor identified by the id and restart it.
   * Extractor identified by id must have been started
   * @param id extractor to be updated
   * @param extData data for updating
   */
  def updateExtractor(id: Long, extData: ExtractorFormInput) : Future[Result] = {
    val extractorId = id
    val entityRef = sharding.entityRefFor(ExtractorGuardianEntity.TypeKey, extractorId.toString)
    val reply = entityRef.askWithStatus(ref => ExtractorGuardianEntity.getStatus(ref))

    val result = reply.flatMap(status =>
      if(status.status != "not started"){
        val extState = formToState(extData)
        val reply = entityRef.askWithStatus(ref => ExtractorGuardianEntity.updateExtractor(extState,ref))
        val result = reply.map( response => {
          logger.info(s"Success updating with $extractorId id")
          Ok(
            Json.toJson(new ExtractorStatusResponse(
              response.status.id.toLong, response.status.status
            ))
          )
        })
        result
      }
      else{
        logger.info(s"Request tried to update extractor with id ${extData.id} but extractor has not been posted")
        Future(BadRequest(JSONError.format(
          Json.obj(
            "id" -> "Extractor with provided id does not exists"
          ))
        ))
      })
    val finalResult = result.recoverWith{
      case exc =>
        exc.printStackTrace()
        logger.info("Failed to update extractor")
        Future(BadRequest(s"Failed to update extractor: ${exc.getMessage}"))
    }
    finalResult
  }

  /**
   * Delete an extractor persistent data and stop the actor in the system
   * if cant found the id it delete the not started instance
   * @param id extractor id
   */
  def deleteExtractor(id: Long): Future[Result] = {

    val entityRef = sharding.entityRefFor(ExtractorGuardianEntity.TypeKey, id.toString)
    entityRef ! ExtractorGuardianEntity.ExterminateExtractor


    val cleanup = new Cleanup(actorSystem)
    val persistenceId: String = PersistenceId(ExtractorGuardianEntity.TypeKey.name, id.toString).toString()

    val result: Future[Result] = cleanup.deleteAll(persistenceId, neverUsePersistenceIdAgain = false).transformWith{
      case Success(_) =>
        Future(NoContent)
      case Failure(exc) =>
        exc.printStackTrace()
        Future(BadRequest(JSONError.format(
          Json.obj(
            "general" -> s"Error deleting extractor data with id $id: ${exc.getMessage}"
          ))))
    }

    val finalResult: Future[Result] = result.recoverWith{
      case exc =>
        logger.info("Failed to delete resource")
        exc.printStackTrace()
        Future(BadRequest(s"Failed to delete resource: ${exc.getMessage}"))
    }
    finalResult

  }
  /**
   * Start an extractor who was stopped using a correct entity id
   * @param id entity id
   * @return status of the current actor
   */
  def startExtractor(id: Long): Future[Result] = {
    val entityRef = sharding.entityRefFor(ExtractorGuardianEntity.TypeKey, id.toString)

    val reply: Future[ExtractorGuardianEntity.Status] =
      entityRef.askWithStatus(ref => ExtractorGuardianEntity.getStatus(ref))
    val result: Future[Result] = reply.transformWith{
      case Success(status) =>
        if (status.status == "stopped" || status.status == "error"){
            val resultStart = entityRef.askWithStatus(ref => ExtractorGuardianEntity.startExtractor(ref))
            resultStart.transformWith{
              case Success(_) =>
                Future(Ok(
                  Json.toJson(new ExtractorStatusResponse(
                    id, "starting"
                  ))
                ))
              case Failure(_) =>
                Future(BadRequest(JSONError.format(
                  Json.obj(
                    "state" -> "Fail while starting extractor"
                  ))))
            }
          }
        else{
          Future(BadRequest(JSONError.format(
            Json.obj(
              "state" -> "Extractor is not stopped or failed"
            ))))
        }
      case Failure(_) =>
        Future(BadRequest(JSONError.format(
          Json.obj(
            "general" -> "Failure while getting extractor"
          ))))
    }
    val finalResult: Future[Result] = result.recoverWith{
      case exc =>
        logger.info("Failed to start extractor")
        exc.printStackTrace()
        Future(BadRequest(s"Failed to start extractor: ${exc.getMessage}"))
    }
    finalResult
  }
  /**
   * Stop an extractor who is running or starting
   * @param id entity id
   * @return status of the current actor
   */
  def stopExtractor(id: Long): Future[Result] = {
    val entityRef = sharding.entityRefFor(ExtractorGuardianEntity.TypeKey, id.toString)

    val reply: Future[ExtractorGuardianEntity.Status] =
      entityRef.askWithStatus(ref => ExtractorGuardianEntity.getStatus(ref))
    val result: Future[Result] = reply.transformWith{
      case Success(status) =>
        if (status.status == "running" || status.status == "starting"){
          val resultStart = entityRef.askWithStatus(ref => ExtractorGuardianEntity.stopExtractor(ref))
          resultStart.transformWith{
            case Success(_) =>
              Future(Ok(
                Json.toJson(new ExtractorStatusResponse(
                  id, "stopped"
                ))
              ))
            case Failure(_) =>
              Future(BadRequest(JSONError.format(
                Json.obj(
                  "state" -> "Couldn't stop extractor"
                ))))
          }
        }
        else{
          Future(BadRequest(JSONError.format(
            Json.obj(
              "state" -> s"Extractor state is ${status.status} not running or starting"
            ))))
        }
      case Failure(_) =>
        Future(BadRequest(JSONError.format(
          Json.obj(
            "general" -> "Failure while getting extractor"
          ))))
    }
    val finalResult: Future[Result] = result.recoverWith{
      case exc =>
        logger.info("Failed to stop extractor")
        exc.printStackTrace()
        Future(BadRequest(s"Failed to stop extractor: ${exc.getMessage}"))
    }
    finalResult
  }

}

