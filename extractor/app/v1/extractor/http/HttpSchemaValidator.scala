package v1.extractor.http


import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, IllegalUriException}
import akka.stream.alpakka.json.scaladsl.JsonReader
import akka.stream.scaladsl._
import akka.util.ByteString
import com.fasterxml.jackson.core.JsonParseException
import play.api.data.validation.{Invalid, Valid, ValidationError, ValidationResult}
import play.api.libs.json.{JsObject, Json}
import v1.extractor.{ExtractorFormInput, SchemaValidator}

import java.time.format.DateTimeFormatter
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

/**
 * Object containing functions for validating Http schema inputs
 */
object HttpSchemaValidator extends SchemaValidator{

  implicit val actorSystem: ActorSystem = akka.actor.ActorSystem("application") //getting play! actor system

  private def checkSchema(rawJson: String, extractor: ExtractorFormInput): mutable.Set[ValidationError] = {
    val json: JsObject = Json.parse(rawJson).as[JsObject]
    val errors: mutable.Set[ValidationError] = mutable.Set[ValidationError]()

    val schema = extractor.dataSchema

    if ( (json \\ schema.sensorIDField).isEmpty) {
      errors.addOne(ValidationError("Can't find sensorID field in JSON"))
    }
    if (schema.longField.nonEmpty && schema.latField.nonEmpty){
      if ( (json \\ schema.latField.get).isEmpty || (json \\ schema.latField.get).isEmpty)
        errors.addOne(ValidationError("Cant find lat long fields in JSON"))
    }
    if ((json \\ schema.timestampField).nonEmpty) {
      val dateValid = (json \\ schema.timestampField).head.asOpt[String] match{
        case Some(date) =>
          try {
            DateTimeFormatter.ISO_DATE_TIME.parse(date)
            true
          }
          catch {
            case _: Throwable => false
        }
        case _ => false
      }
      if (! dateValid)
        errors.addOne(ValidationError("Timestamp is not a valid ISO date"))
    } else {
      errors.addOne(ValidationError("Can't find timestamp field in JSON"))
    }

    schema.measures.foreach(measure => {
      val measureField = (json \\ measure.field)
      if (measureField.nonEmpty) {
        val measureNumber = measureField.head.validate[String]
        if (! measureNumber.contains("")){
          measureNumber.asEither match {
            case Left(numb) =>
              val measureAsDouble = measureField.head.validate[Double]
              measureAsDouble.asEither match{
                case Left(value) =>
                  errors.addOne(ValidationError(s"Number expected but other type found in ${measure.field}"))
                case Right(value) =>
              }
            case Right(numb) =>
              val isNotDouble = numb.toDoubleOption.isEmpty
              if (isNotDouble)
                errors.addOne(ValidationError(s"Number expected but other type found in ${measure.field}"))

            }
        }
      }else {
        errors.addOne(ValidationError(s"Measure ${measure.name} not found in source"))
      }})
    errors

  }

  def extractEntityData(response: HttpResponse): Source[ByteString, _] = {
    response match {
      case HttpResponse(OK, _, entity, _) => entity.dataBytes
      case notOkResponse =>
        Source.failed(new RuntimeException(s"illegal response $notOkResponse"))
    }
  }

  /**
   * Validate a HTTP extractor with his font
   * @param extractor data of the extractor
   * @param maxNumSensor config setting
   * @return
   */
  def validate(extractor: ExtractorFormInput, sensorsToCheck: Int, maxNumSensor: Int): ValidationResult = {

    val inputConfig = extractor.ioConfig.inputConfig
    val errors: mutable.Set[ValidationError] = collection.mutable.Set()

    if (inputConfig.jsonPath.isEmpty) {
      errors.addOne(ValidationError("You must set jsonPath variable for HttpExtractor"))
    }

    if (inputConfig.freq.isEmpty)
      errors.addOne(ValidationError("You must set freq variable for HttpExtractor"))

    if (errors.isEmpty){
      val httpRequest = HttpRequest(uri = inputConfig.address)
      val futureResponse: Future[mutable.Set[ValidationError]] = {
        try{
          Source
            .single(httpRequest)
            .mapAsync(1)(Http()(actorSystem).singleRequest(_)) //: HttpResponse
            .flatMapConcat(extractEntityData)
            .via(JsonReader.select(inputConfig.jsonPath.get))
            .via(JsonFraming.objectScanner(maxNumSensor))
            .take(sensorsToCheck)
            .map(_.utf8String)
            .map(rawJson => checkSchema(rawJson, extractor))
            .recover{
              case e: IllegalUriException =>
                mutable.Set(ValidationError("Cannot determine target address"))
              case e: JsonParseException =>
                mutable.Set(ValidationError("Parser failed to read JSON"))
              case e: RuntimeException =>
                e.printStackTrace()
                mutable.Set(ValidationError("Source extraction error while parsing JSON (check content type and jsonPath)"))
              case other =>
                other.printStackTrace()
                mutable.Set(ValidationError("Error while validating the schema"))
            }
            .runWith(Sink.head)
        }catch{
          case e: Throwable =>
            e.printStackTrace()
            Future(
              errors.addOne(
                ValidationError(s"Failure while doing schema mapping ${e.getMessage}")
              )
            )
        }

      } //SOlo toma un elemento
      //Tengo que evitar que sea síncrono
      val response = Await.result(futureResponse,3.seconds)
      if (response.isEmpty) {
        Valid
      } else
        Invalid(response.toSeq)
    }
    else{
      Invalid(errors.toSeq)
    }

  }
}
