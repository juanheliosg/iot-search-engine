package v1.querier


import play.api.Configuration
import play.api.libs.json.{Format, JsError, JsSuccess, Json, OFormat}
import play.api.libs.ws._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


case class DruidRecord(
                      seriesID: String,
                      sensorID: String,
                      __time: String,
                      address: String,
                      city: String,
                      country: String,
                      description: String,
                      measure: BigDecimal,
                      measure_name: String,
                      unit: String,
                      measure_desc: String,
                      name: String,
                      region: String,
                      sampling_unit: String,
                      sampling_freq: String,
                      coordinates: List[String] = List("",""),
                      tags: List[String],
                      avg_agg: Option[BigDecimal] = None,
                      stddev_agg: Option[BigDecimal] = None,
                      sum_agg: Option[BigDecimal] = None,
                      max_agg: Option[BigDecimal] = None,
                      min_agg: Option[BigDecimal] = None,
                      )

object DruidRecord{
  implicit val druidReader: OFormat[DruidRecord] = Json.format[DruidRecord]
}

/**
 * Druid Errorm mapper
 * @param error name of the error
 * @param errorMessage message
 * @param errorClass type of error
 * @param host druid host
 */
case class DruidError(
                     error: String,
                     errorMessage: String,
                     errorClass: String = "",
                     host: String =""
                     )
object DruidError{
  implicit val druidErrorReader: Format[DruidError] = Json.format[DruidError]
}

class DruidAPIManager @Inject() (config: Configuration, ws: WSClient, implicit val ec: ExecutionContext){
  val request: WSRequest = ws.url(config.underlying.getString("querier.druid-url"))

  /**
   * Return records after query by SQL
   * @param sqlQuery
   * @return
   */

  def getRecords(sqlQuery: String): Future[Either[List[DruidRecord],DruidError]] = {
    val data = Json.obj(
      "query" -> sqlQuery
    )

    val response: Future[WSResponse] = request.post(data)
    response.map(resp => {
      if(resp.status == 200){
        resp.json.validate[List[DruidRecord]] match{
          case JsSuccess(value, path) => Left(value)
          case JsError(errors) =>
            Right(DruidError("Json parsing error",errors.flatMap(
              er => er._2.flatMap(valError => valError.message).mkString("\n")).mkString("\n"))
            )
        }
      }
      else{
        Right(resp.json.validate[DruidError] match{
          case JsSuccess(value, path) => value
          case JsError(errors) =>
            DruidError("Json parsing error",errors.flatMap(
              er => er._2.flatMap(valError => valError.message).mkString("\n")).mkString("\n"))
        }
        )}
      }
  )
  }
}

