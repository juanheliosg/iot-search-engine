package v1.extractor

import akka.actor.ActorSystem
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity}
import akka.pattern.StatusReply
import akka.persistence.cassandra.cleanup.Cleanup
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.PersistenceQuery
import akka.persistence.typed.PersistenceId
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import akka.util.Timeout
import org.slf4j.LoggerFactory
import play.api.Configuration
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{AbstractController, ControllerComponents, Result}
import v1.extractor.actors.{ExtractorGuardianEntity, PlayActorConfig}
import v1.extractor.models.extractor.config.{HttpInputConfig, InputConfig}
import v1.extractor.models.extractor.{ExtractorGetResponse, ExtractorState, ExtractorStatusResponse, IOConfig}

import java.time.{LocalDateTime, ZoneId, ZonedDateTime}
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Random, Success}


/**
 * Implements extractors logic and communication with Akka actors
 * @param cc controller components
 * @param sharding Cluster Sharding
 * @param config Play! Configuration
 * @param actorSystem Play! Actor system
 */
@Singleton
class ExtractorServiceImpl @Inject() (val cc: ControllerComponents, val sharding: ClusterSharding, val config: Configuration
                                      , val actorSystem: ActorSystem, implicit val mat: Materializer ) extends AbstractController(cc){

  val duration = java.time.Duration.ofSeconds(config.get[Long]("extractor.timeout-seconds"))
  implicit private val timeout: Timeout =
    Timeout.create(duration)
    //Timeout.create(actorSystem.settings.config.getDuration("extractor.timeout"))
  implicit private val executionContext: ExecutionContext = cc.executionContext

  val query: CassandraReadJournal =
    PersistenceQuery(actorSystem).readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)

  private val logger = LoggerFactory.getLogger(getClass)
  private val playActorConfig = new PlayActorConfig(actorSystem, config)
  private val rng = new Random()
  private var numOfRepeatedPost = 0
  private val collisionThreshold = 10

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
    ExtractorState(extData.dataSchema,IoConfig, extractorType, extData.metadata)
  }

  /**
   * Generate a likely unique id based in a timestamp value and a random value.
   * It is a 8 number string. The first 4 characters are selected from unix epoch time
   * The last 4 from a rng number
   * @return a 8 digit string
   */
   def generateUniqueId(): String = {
    val zdt: ZonedDateTime = LocalDateTime.now().atZone(ZoneId.of("America/Denver"))
    val millis = zdt.toInstant.toEpochMilli.toString
    val timestampPart = millis.substring(millis.length-4,millis.length)
    rng.setSeed(millis.toLong)
    val randomPart = for(i <- 1 to 4) yield rng.nextInt(10).toString

    timestampPart+randomPart.mkString("")
  }

  /**
   * Generic Ok response for getting or updating an extractor
   * @param extractorID id
   * @param status status
   * @param state extractor internal state
   * @return
   */
  private def returnFullExtractor(extractorID: String, status: String, state: ExtractorState) = {
    Ok(
      Json.toJson(
        new ExtractorGetResponse(
          extractorID,
          status,
          state.extractorType.toString,
          state.schema,
          state.config,
          state.metadata
        )))
  }

  /**
   * Get an extractor data using a correct entity id
   * @param id entity id
   * @return extractorGetResponse object containing id, status, type, schema and config
   */
  def getExtractor(id: String): Future[Result] = {
    val extractorId = id
    val entityRef = sharding.entityRefFor(ExtractorGuardianEntity.TypeKey, extractorId)

    val reply: Future[ExtractorGuardianEntity.Summary] =
      entityRef.askWithStatus(ref => ExtractorGuardianEntity.getExtractor(ref))
    reply.transformWith{
      case Success(summary) => Future(
        returnFullExtractor(extractorId,summary.status.status,summary.extractorState))

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

    val isNotUnique = checkIfUrlUnique(extData.ioConfig.inputConfig.address)
    if (isNotUnique){
      return Future(BadRequest(JSONError.format(
        Json.obj(
          "ioconfig.inputConfig.address" -> "Other extractor exists with the same URL"
        ))
      ))
    }

    val extractorId = generateUniqueId()
    val entityRef = sharding.entityRefFor(ExtractorGuardianEntity.TypeKey,extractorId)
    val reply = entityRef.askWithStatus(ref => ExtractorGuardianEntity.getStatus(ref))

    val result = reply.flatMap(status =>
      if (status.status != "not started"){ //change to enum
        logger.info(s"ID collision with id $extractorId ")
        numOfRepeatedPost += 1
        if (numOfRepeatedPost > collisionThreshold){
          Future(InternalServerError(JSONError.format(
            Json.obj(
              "id" -> "More than 10 collisions, contact server admin"
            ))
          ))
        }
        else {
          postExtractor(extData)
        }
      }
      else{
        numOfRepeatedPost = 0
        val newExtractor = sharding.entityRefFor(ExtractorGuardianEntity.TypeKey, extractorId)
        val extState = formToState(extData)
        val reply = newExtractor.askWithStatus(ref => ExtractorGuardianEntity.updateExtractor(extState,ref))
        val result = reply.map( response => {
          logger.info(s"Success creating extractor with $extractorId id")
          newExtractor.ask(ref => ExtractorGuardianEntity.startExtractor(ref))
          Created(
            Json.toJson(
              new ExtractorGetResponse(
                extractorId,
                response.status.status,
                response.extractorState.extractorType.toString,
                response.extractorState.schema,
                response.extractorState.config,
                response.extractorState.metadata
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
   * @param id id to extract
   * @param extData extData for updating the actor
   * @return
   */
  def updateExtractor(id: String, extData: ExtractorFormInput) : Future[Result] = {

    val isNotUnique = checkIfUrlUnique(extData.ioConfig.inputConfig.address, exclusionId = Some(id))
    if (isNotUnique){
      return Future(BadRequest(JSONError.format(
        Json.obj(
          "ioconfig.inputConfig.address" -> "Other extractor exists with the same URL"
        ))
      ))
    }

    val extractorId = id
    val entityRef = sharding.entityRefFor(ExtractorGuardianEntity.TypeKey, extractorId)
    val reply = entityRef.askWithStatus(ref => ExtractorGuardianEntity.getStatus(ref))

    val result = reply.flatMap(status =>
      if(status.status != "not started"){
        val extState = formToState(extData)
        val reply = entityRef.askWithStatus(ref => ExtractorGuardianEntity.updateExtractor(extState,ref))
        val result = reply.map( response => {
          logger.info(s"Success updating with $extractorId id")
          Ok(
            Json.toJson(new ExtractorGetResponse(
            extractorId,
            response.status.status,
            response.extractorState.extractorType.toString,
            response.extractorState.schema,
            response.extractorState.config,
            response.extractorState.metadata
          ))
          )
        })
        result
      }
      else{
        logger.info(s"Request tried to update extractor with id $id but extractor has not been posted")
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
  def deleteExtractor(id: String): Future[Result] = {

    val entityRef = sharding.entityRefFor(ExtractorGuardianEntity.TypeKey, id)
    entityRef ! ExtractorGuardianEntity.ExterminateExtractor


    val cleanup = new Cleanup(actorSystem)
    val persistenceId: String = PersistenceId(ExtractorGuardianEntity.TypeKey.name, id).toString()

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
  def startExtractor(id: String): Future[Result] = {
    val entityRef = sharding.entityRefFor(ExtractorGuardianEntity.TypeKey, id)

    val reply: Future[ExtractorGuardianEntity.Status] =
      entityRef.askWithStatus(ref => ExtractorGuardianEntity.getStatus(ref))
    val result: Future[Result] = reply.transformWith{
      case Success(status) =>
        if (status.status == "stopped" || status.status == "error"){
            val resultStart = entityRef.askWithStatus(ref => ExtractorGuardianEntity.startExtractor(ref))
            resultStart.transformWith{
              case Success(response) =>
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
  def stopExtractor(id: String): Future[Result] = {
    val entityRef = sharding.entityRefFor(ExtractorGuardianEntity.TypeKey, id)

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

  /**
   * Returns the entityID ref part of the persistenceID
   * @param persistenceID id to retrieve
   * @return entityID
   */
  private def getEntityId(persistenceID: String) = {
    val typeLenght = ExtractorGuardianEntity.TypeKey.name.length
    val sizePersistenceId = typeLenght + 1
    persistenceID.substring(sizePersistenceId)
  }

  private def getNameStatus(entityID: String): Future[JsObject] = {
    val entityRef = sharding.entityRefFor(ExtractorGuardianEntity.TypeKey, entityID)

    val reply: Future[ExtractorGuardianEntity.Summary] =
      entityRef.askWithStatus(ref => ExtractorGuardianEntity.getExtractor(ref))

    reply.transformWith({
      case Success(summary) =>
        Future(Json.obj(
          "id" -> entityID,
          "name" -> summary.extractorState.metadata.name,
          "state" -> summary.status.status
        ))
      case Failure(StatusReply.ErrorMessage(_)) =>
        Future(JSONError.format(
          Json.obj(
            "id" -> s"Couldnt get $entityID extractor"
          ))
        )
    })
  }

  /**
   * Get numToExtract extractors. Order is not guaranteed.
   * @param numToExtract Number of extractors to retry
   * @return
   */
  def getAllExtractors(numToExtract: Long): Future[Result] = {

    val extractors: Future[Seq[JsObject]] = query
      .currentPersistenceIds()
      .filter( id =>
        id.contains(ExtractorGuardianEntity.TypeKey.name) && ! id.contains("sharding")
      )
      .take(numToExtract)
      .map(getEntityId)
      .mapAsync(1)(getNameStatus)
      .runWith(Sink.seq)

    extractors.flatMap(extList =>
      Future(Ok(Json.obj(
        "items" -> extList.size,
        "extractors" -> extList.map(el => el)
        ))
    ))

  }

  /**
   * Check if the given url is the same as other entity id url
   * @param url ulr to check
   * @param entityID entity id to retrieve url
   * @return
   */
  private def isUrlTheSame(url: String, entityID: String): Future[Boolean] = {
    val entityRef = sharding.entityRefFor(ExtractorGuardianEntity.TypeKey, entityID)

    val reply: Future[ExtractorGuardianEntity.Summary] =
      entityRef.askWithStatus(ref => ExtractorGuardianEntity.getExtractor(ref))

    reply.flatMap (response => {
      Future(response.extractorState.config.inputConfig.address == url)
      }
    )
  }

  /**
   * Check if the given url is unique in the system.
   * THis operation requires scanning all the persistence Ids in the system.
   * There is no index o table avalaible to this in a more efficient way
   * @param url url to check
   * @param exclusionId id to be excluded from search. Can be used checking update operations
   * @return
   */
  private def checkIfUrlUnique(url: String, exclusionId: Option[String] = None): Boolean = {

    val extractors: Future[Seq[Boolean]] = query
      .currentPersistenceIds()
      .filter( id => {
        val isExtractor = id.contains(ExtractorGuardianEntity.TypeKey.name) && ! id.contains("sharding")
        if (exclusionId.nonEmpty)
          isExtractor && getEntityId(id) != exclusionId.get
        else isExtractor
      }
      )
      .map(getEntityId)
      .mapAsync(1)(isUrlTheSame(url,_))
      .runWith(Sink.seq)

    val result = Await.result(extractors, 10.seconds)

    result.contains(true)

  }


}

