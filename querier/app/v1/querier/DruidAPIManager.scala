package v1.querier


import akka.stream.Materializer
import akka.stream.scaladsl.{JsonFraming, Sink, Source}
import akka.util.ByteString
import play.api.Configuration
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json._
import play.api.libs.ws._
import v1.querier.models.QueryResponse

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class DruidRecord(
                      seriesID: String,
                      sensorID: String,
                      __time: Option[String],
                      address: String,
                      city: String,
                      country: String,
                      description: String,
                      measure: Option[BigDecimal],
                      measure_name: String,
                      unit: String,
                      measure_desc: String,
                      name: String,
                      region: String,
                      sampling_unit: String,
                      sampling_freq: Int,
                      lat: String,
                      long: String,
                      tags: Seq[String],
                      avg_agg: Option[BigDecimal] = None,
                      stddev_agg: Option[BigDecimal] = None,
                      sum_agg: Option[BigDecimal] = None,
                      max_agg: Option[BigDecimal] = None,
                      min_agg: Option[BigDecimal] = None
                      )


/**
 * Druid Error mapper
 *
 * @param error        name of the error
 * @param errorMessage message
 * @param errorClass   type of error
 * @param host         druid host
 */
case class DruidError(
                       error: String,
                       errorMessage: String,
                       errorClass: String = "",
                       host: Option[String] = None
                     )

object DruidError {
  implicit val JSONErrorReader: Reads[DruidError] = Json.reads[DruidError]

}
case class DruidCountField(name: String, value: Long)
object DruidCountField{
  implicit val druidGeneralReads :Reads[DruidCountField] = Json.reads[DruidCountField]
  implicit val druidGeneralWrites :Writes[DruidCountField] = Json.writes[DruidCountField]
}

class DruidAPIManager @Inject()
(config: Configuration, ws: WSClient, implicit val ec: ExecutionContext,implicit val mat: Materializer) extends GeneralAPIManager
    with QueryProcessor {
  val request: WSRequest = ws.url(config.underlying.getString("querier.druid-url"))
  private val datasource = config.underlying.getString("querier.datasource")

  final case class DruidRecordException(private val message: String = "",
                                        private val cause: Throwable = None.orNull) extends Exception(message,cause)

  private def parseTSResponse(rawJson: String): DruidRecord = {
    val rawParsedJson = Json.parse(rawJson)
    DruidRecord((rawParsedJson \ "seriesID").as[String],
      (rawParsedJson \ "sensorID").as[String] ,
      (rawParsedJson \ "__time").asOpt[String] ,
      (rawParsedJson \ "address").as[String],
      (rawParsedJson \ "city").as[String],
      (rawParsedJson \ "country").as[String],
      (rawParsedJson \ "description").as[String],
      (rawParsedJson \ "measure").asOpt[BigDecimal] ,
      (rawParsedJson \ "measure_name").as[String],
      (rawParsedJson \ "unit").as[String],
      (rawParsedJson \ "measure_desc").as[String],
      (rawParsedJson \ "name").as[String],
      (rawParsedJson \ "region").as[String],
      (rawParsedJson \ "sampling_unit").as[String],
      (rawParsedJson \ "sampling_freq").as[Int],
      (rawParsedJson \ "lat").as[String],
      (rawParsedJson \ "long").as[String],
      (rawParsedJson \ "tags").as[String]
        .replace("]","")
        .replace("[","")
        .replace("\"","")
        .split(","),
      (rawParsedJson \ "avg_agg").asOpt[BigDecimal],
      (rawParsedJson \ "stddev_agg").asOpt[BigDecimal],
      (rawParsedJson \ "sum_agg").asOpt[BigDecimal],
      (rawParsedJson \ "max_agg").asOpt[BigDecimal],
      (rawParsedJson \ "min_agg").asOpt[BigDecimal]
    )


  }

  def streamProcessing(stream: Source[ByteString, _]): Either[Future[Seq[QueryResponse]],List[JSONError]] = {
    val finalRes: Future[Seq[QueryResponse]] =
      stream.via(JsonFraming.objectScanner(Int.MaxValue))
        .map(_.utf8String)
        .map(parseTSResponse)
        .groupBy(Int.MaxValue,_.seriesID)
        .map(dr => getIntermediateQueryResponse(dr))
        .reduce( (finalResp,qr) => {
          reduceQueryResponseStream(finalResp,qr)
        })
        .mergeSubstreams
        .map(_.toQueryResponse)
        .runWith(Sink.seq)(mat)
    Left(finalRes)
  }
  /**
   * Return records after query by SQL but streaming the response
   * @param sqlQuery
   * @return
   */

  def getRecordsWithStream(sqlQuery: String): Future[Either[Future[Seq[QueryResponse]],List[JSONError]]] = {
    val data = Json.obj(
      "query" -> sqlQuery
    )
    val response: Future[WSResponse] = request.withMethod("POST").withBody(data).stream()
    response.map(
      res => {
        if (res.status == 200) {
          try{
            val streamSource: Source[ByteString, _] = res.bodyAsSource
            streamProcessing(streamSource)
          }catch {
            case e: Exception =>
              Right(List(JSONError("Response processing error", e.getMessage)))
          }
        }
        else{
          try{
            res.json.validate[DruidError] match {
              case JsSuccess(value, path) => Right(JSONError.toErrorList(value))
              case JsError(errors) =>
                Right(List(JSONError("Json parsing error after error result.", errors.flatMap(er => er._2.map(_.message).mkString(" ")
                ).mkString(""))))
            }
          }catch{
            case _ => Right(List(JSONError("Unknown query error","Check broker services of druid" )))
          }

        }
      }
    )
  }

  /**
   * Get a row with field  number of rows with that value
   * @param field
   * @return
   */
  def getCountField(field: String):Future[Either[List[DruidCountField], List[JSONError]]] = {
    val data = Json.obj(
      "query" -> s"""SELECT $field as name, COUNT(*) as "value"  FROM $datasource GROUP BY $field"""
    )
    postGeneralQuery[DruidCountField,DruidError](data, request, JSONError.toErrorList)(DruidCountField.druidGeneralReads,ec, DruidError.JSONErrorReader)
  }
}



