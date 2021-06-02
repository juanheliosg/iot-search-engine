package v1.extractor.http

import akka.Done
import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.alpakka.json.scaladsl.JsonReader
import akka.stream.scaladsl.Framing.FramingException
import akka.stream.scaladsl.{JsonFraming, Keep, RestartSource, Sink, Source}
import akka.stream.{KillSwitch, KillSwitches, Materializer, RestartSettings}
import akka.util.ByteString
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.jsfr.json.exception.JsonSurfingException
import play.api.libs.concurrent.ExecutionContextProvider
import play.api.libs.json.Json
import spray.json.{DefaultJsonProtocol, DeserializationException, JsNumber, JsObject, JsString, JsValue, JsonParser}
import v1.extractor.ExtractionActivity
import v1.extractor.actors.Extractor.{StreamError, StreamRunning}
import v1.extractor.actors.{Extractor, PlayActorConfig}
import v1.extractor.models.extractor.{DataSchema, ExtractorState, MeasureField}
import v1.extractor.models.metadata.Metadata

import java.time.format.DateTimeFormatter
import java.time.{ZoneId, ZonedDateTime}
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.{DurationInt, FiniteDuration, MILLISECONDS}
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

/**
 * Object implementing the Akka stream for extracting data in json format from an HTTP endpoint
 */
case object HTTPExtractor extends ExtractionActivity with DefaultJsonProtocol {
  /**
   * Extract entity data from http response in an akka stream
   * @param response http response
   * @param supervisor supervisor of the stream
   * @return
   */
  def extractEntityData(response: HttpResponse, supervisor: ActorRef[Extractor.Command])
                       (implicit mat: Materializer): Source[ByteString,_] = {
    response match{
      case HttpResponse(OK, _, entity, _) =>
        entity.dataBytes
      case failedResponse =>
        failedResponse.discardEntityBytes()

        supervisor ! StreamError(s"Illegal HTTP response with code ${failedResponse._1}")
        Source.failed(new RuntimeException(s"illegal response with code ${failedResponse._1}"))
    }
  }

  /**
   * Create a row of data representing a point in a time series
   * @param entityID
   * @param measureIDString
   * @param sensorID
   * @param timestamp
   * @param measure
   * @param measure_index
   * @param measure_data
   * @param latValue
   * @param longValue
   * @return
   */
  def createRow(entityID :String, measureIDString: String, sensorID: String, timestamp:String, measure: Double, measure_index: Int,
                measure_data: MeasureField, latValue: String, longValue: String): JsObject = {
    JsObject(
      Map(
        "seriesID" -> new JsString(DataSchema.composeUniqueSerieID(entityID, measureIDString, sensorID)),
        "sensorID" -> new JsString(sensorID),
        "timestamp" -> new JsString(timestamp),
        "sourceID" -> new JsString(entityID),
        "measure" -> new JsNumber(measure),
        "measureID" -> new JsNumber(measure_index),
        "measure_name" -> new JsString(measure_data.name),
        "unit" -> new JsString(measure_data.unit),
        "measure_desc" -> new JsString(measure_data.description.getOrElse("")),
        "lat" -> new JsString(latValue),
        "long" -> new JsString(longValue)
      ))
  }

  /**
   * Convert a date to UTC timestamp string if is not in that format
   * @param date
   * @return
   */
  def toUTC(date: String): String = {
    if (date.tail == "Z"){ //timestmap is with UTC timestamp
      date
    }
    else{
      try{
        val time: ZonedDateTime = ZonedDateTime.parse(date)
        time.withZoneSameInstant(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      }catch{
        case _ =>
          //Si no tiene zona horaria no podemos tener ninguna fuente de verdad clara y por tanto tomamos el tiempo actual.
          ZonedDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      }
    }
  }

  /**
   * Create a Json Measure from the original source schema using recursive lookup
   *
   * The json measure contains the following fields:
   * seriesID -> unique identifier for the temporal serie compound by the sensor, source and measure ID
   * sensorID -> sensorID
   * timestamp -> in ISO Date
   * measure -> measure in double format
   * measureID -> measureID generated by using the position of the measures in the array. Is not unique
   * name -> textual identifier for measure
   * lat -> latitude of the sensor
   * long -> longitude of the sensor
   *
   * @param rawSensorData raw string with json data
   * @param dataSchema mapping data
   * @return Seq[JsValue] sequence of Json formatted objects
   */
  def parseRecursiveJSON(entityID: String, rawSensorData: String, dataSchema: DataSchema): Seq[JsValue] = {
    val jsonMeasures = Json.parse(rawSensorData)
    val sensorID = (jsonMeasures \\ dataSchema.sensorIDField).head.as[String].replace("\"","")

    val latValue = dataSchema.latField match {
      case Some(value) => (jsonMeasures \\ value).head.as[String]
      case None => ""
    }
    val longValue = dataSchema.longField match {
      case Some(value) => (jsonMeasures \\ value).head.as[String]
      case None => ""
    }

    val measures = dataSchema.measures.zipWithIndex.flatMap{case (measure, index) =>
      val measureIDString = index.toString
      val strMeasure = (jsonMeasures \\ measure.field).head.asOpt[String]
      val timestamp = (jsonMeasures \\ dataSchema.timestampField).head.as[String]
      val utcTimestamp = toUTC(timestamp)

      if (strMeasure.isEmpty) {
        val numbMeasure = (jsonMeasures \\ measure.field).head.asOpt[Double]
        if (numbMeasure.isEmpty)
          None
        else{
          Some(
            createRow(entityID,measureIDString,sensorID,utcTimestamp,numbMeasure.get,index,measure,latValue, longValue)
          )
        }
      } else {
        if (strMeasure.get != ""){
          Some(
            createRow(entityID,measureIDString,sensorID,utcTimestamp,strMeasure.get.toDouble,index,measure,latValue, longValue)
          )
        }
        else
          None
      }
    }
    measures
  }


  /**
   * Inject metadata information to the measure record in a flattened way.
   * @param record measure record
   * @param metadata object containing metadata information
   * @return a new JsValue with the metadata information
   */
  private def injectMetadata(record: JsValue, metadata: Metadata): JsValue  = {
    val mapMetadata = metadata.metadataMap()
    val merged = record.asJsObject.fields ++ mapMetadata.toSeq
    JsObject(merged)
  }

  /**
   * Transforms data from an HTTP endpoint to a Kafka topic following the dataSchema defined in extData
   *
   * @param extData extData with IOConfig and DataSchema
   * @param supervisor stream supervisor actor
   * @param playConfig config values of Play! for getting the actorSystem and other config values
   * @return
   */
  override def extraction(entityID: String, extData: ExtractorState,
                          supervisor: ActorRef[Extractor.Command], playConfig: PlayActorConfig): KillSwitch = {

    implicit val actorSystem: ActorSystem = playConfig.actorSystem
    implicit val executionContext: ExecutionContextExecutor = new ExecutionContextProvider(actorSystem).get()

    val kafkaConfig = extData.config.kafkaConfig
    val inputConfig = extData.config.inputConfig
    val httpConfig = inputConfig.httpConfig.get

    val producerConfig = actorSystem.settings.config.getConfig("akka.kafka.producer")
    val producerSettings = ProducerSettings(producerConfig, new StringSerializer, new StringSerializer)
      .withBootstrapServers(bootstrapServers = kafkaConfig.server)

    val httpRequest = HttpRequest(uri = inputConfig.address)
    val duration = new FiniteDuration(httpConfig.freq, MILLISECONDS) //hardcoded milliseconds

    val restartSettings = RestartSettings(
      minBackoff = 1.second,
      maxBackoff = 60.second,
      randomFactor = 0.2
    ).withMaxRestarts(20, 5.minute)

    /**
     * Internal stream for managing external  connections
     * @param httpRequest request for streaming
     * @return
     */
    def getHttpResponse(httpRequest: HttpRequest): Future[HttpResponse] = {
      RestartSource.withBackoff(restartSettings){ () =>
        val response = Http()(actorSystem).singleRequest(httpRequest)
        Source.future(response)
      }
        .runWith(Sink.head)
        .recover {
          case e =>
            throw e
        }
    }

    val (killSwitch, future): (KillSwitch, Future[Done]) =
      Source
        .tick(1.seconds, duration, httpRequest)
        .mapAsync(parallelism=4)(getHttpResponse)
        .flatMapConcat(extractEntityData(_, supervisor))
        .via(JsonReader.select(httpConfig.jsonPath))
        .via(JsonFraming.objectScanner(playConfig.config.get[Int]("extractor.max-sensor-per-extractor")))
        .map(_.utf8String)
        .mapConcat(str => parseRecursiveJSON(entityID, str, extData.schema))
        .map(value => injectMetadata(value, extData.metadata))
        .map(_.compactPrint)
        .map(elem => new ProducerRecord[String, String](kafkaConfig.topic, elem))
        .recover {
          case frameExcept: FramingException =>
            supervisor ! StreamError(s"Json framing error (invalid JSON or maximumObject length")
            throw frameExcept
          case pathExcept: JsonSurfingException =>
            supervisor ! StreamError("Error with jsonPath or invalid Json format")
            throw pathExcept
          case desExcept: DeserializationException =>
            supervisor ! StreamError("Error deserializing JSON into measurements")
            throw desExcept
          case e: Exception =>
            supervisor ! StreamError(s"${e.getMessage}")
            throw e
        }
        .viaMat(KillSwitches.single)(Keep.right)
        .toMat(Producer.plainSink(producerSettings))(Keep.both)
        .run()


    future.recover{
      case e =>
        supervisor ! StreamError(s"${e.getMessage}")
    }

  //Stream future is never completed because of tick so
    //we wait until first tick is done and then

    val completeFuture = Future{TimeUnit.SECONDS.sleep(
      playConfig.config.get[Long]("extractor.timeout-seconds")
    )}
    completeFuture.onComplete{
      case Success(value) => supervisor ! StreamRunning
      case Failure(e) => supervisor ! StreamError(s"${e.getMessage}")
    }



    killSwitch
  }

}
