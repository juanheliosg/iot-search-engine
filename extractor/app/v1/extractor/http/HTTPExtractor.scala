package v1.extractor.http

import akka.Done
import akka.actor.typed.ActorRef
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.{KillSwitch, KillSwitches}
import akka.stream.alpakka.json.scaladsl.JsonReader
import akka.stream.scaladsl.Framing.FramingException
import akka.stream.scaladsl.{JsonFraming, Keep, Source}
import akka.util.ByteString
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.jsfr.json.exception.JsonSurfingException
import play.api.libs.concurrent.ExecutionContextProvider
import spray.json.{DefaultJsonProtocol, DeserializationException, JsNumber, JsObject, JsString, JsValue, JsonParser}
import v1.extractor.actors.Extractor.{StreamError, StreamRunning}
import v1.extractor.actors.{Extractor, PlayActorConfig}
import v1.extractor.http.HttpSchemaValidator.actorSystem
import v1.extractor.{DataSchema, ExtractionActivity, ExtractorState}

import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.concurrent.duration.{DurationInt, FiniteDuration, MILLISECONDS}
import scala.util.{Failure, Success}


case object HTTPExtractor extends ExtractionActivity with DefaultJsonProtocol {
  /**
   * Extract entity data from http response in an akka stream
   * @param response http response
   * @param supervisor supervisor of the stream
   * @return
   */
  def extractEntityData(response: HttpResponse,supervisor: ActorRef[Extractor.Command]): Source[ByteString,_] = {
    response match{
      case HttpResponse(OK, _, entity, _) =>
        entity.dataBytes
      case notOkResponse =>
        supervisor ! StreamError(s"Illegal HTTP response")
        Source.failed(new RuntimeException(s"illegal response $notOkResponse"))
    }
  }

  /**
   * Create a Json Measure from the original source schema.
   * The json measure contains the following fields:
   * seriesID -> unique identifier for the temporal serie compound by the sensor, source and measure ID
   * sensorID -> sensorID
   * timestamp -> in ISO Date
   * measure -> measure in double format
   * measureID -> measureID unique identifier
   * name -> textual identifier for measure
   *
   * @param rawSensorData raw string with json data
   * @param dataSchema mapping data
   * @return Seq[JsValue]
   */
  def parseJSON(rawSensorData: String, dataSchema: DataSchema): Seq[JsValue] = {
      val jsonMeasures = JsonParser(rawSensorData).asJsObject.fields
      val sensorID = jsonMeasures.get(dataSchema.sensorIDField).get.toString().replace("\"","")
      val stringSourceID = dataSchema.sourceID.toString
      val measures = dataSchema.measures.map(measure => {
        val measureIDString = measure.measureID.toString
        JsObject(
          Map(
            "seriesID" -> new JsNumber(dataSchema.composeUniqueSerieID(stringSourceID, measureIDString, sensorID)),
            "sensorID" -> new JsNumber(sensorID.toLong),
            "timestamp" -> jsonMeasures(dataSchema.timestampField),
            "sourceID" -> new JsNumber(dataSchema.sourceID),
            "measure" -> new JsNumber(jsonMeasures(measure.field).convertTo[String].toDouble),
            "measureID" -> new JsNumber(measure.measureID),
            "name" -> new JsString(measure.name)
          ))
      })
    if (measures.isEmpty) throw new RuntimeException("Measure empty")
      measures

  }


  /**
   * Transforms data from an HTTP endpoint to a Kafka topic following the dataSchema defined in extData
   * @param extData extData with IOConfig and DataSchema
   * @param supervisor stream supervisor actor
   * @param playConfig config values of Play! for getting the actorSystem and other config values
   * @return
   */
  override def extraction(extData: ExtractorState,
                          supervisor: ActorRef[Extractor.Command], playConfig: PlayActorConfig): KillSwitch = {

    implicit val actorSystem = playConfig.actorSystem
    implicit val executionContext: ExecutionContextExecutor = new ExecutionContextProvider(actorSystem).get()

    val kafkaConfig = extData.config.kafkaConfig
    val inputConfig = extData.config.inputConfig
    val httpConfig = inputConfig.httpConfig.get

    val producerConfig = actorSystem.settings.config.getConfig("akka.kafka.producer")
    val producerSettings = ProducerSettings(producerConfig, new StringSerializer, new StringSerializer)
      .withBootstrapServers(bootstrapServers = kafkaConfig.server)

    val httpRequest = HttpRequest(uri = inputConfig.address)
    val duration = new FiniteDuration(httpConfig.freq, MILLISECONDS) //hardcoded milliseconds

    val (killSwitch, future): (KillSwitch, Future[Done]) =
      Source
        .tick(1.seconds, duration, httpRequest)
        .mapAsync(1)(
          Http()(actorSystem).singleRequest(_))
        .flatMapConcat(extractEntityData(_, supervisor))
        .via(JsonReader.select(httpConfig.jsonPath))
        .via(JsonFraming.objectScanner(playConfig.config.get[Int]("extractor.max-sensor-per-extractor")))
        .map(_.utf8String)
        .mapConcat(str => parseJSON(str, extData.schema))
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
