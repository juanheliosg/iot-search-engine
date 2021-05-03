package v1.querier


import play.api.Configuration
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.libs.ws._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.DurationLong

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
                      name: String,
                      region: String,
                      sampling_unit: String,
                      tags: List[String],
                      unit: String,
                      avg_agg: Option[BigDecimal] = None,
                      stddev_agg: Option[BigDecimal] = None,
                      sum_agg: Option[BigDecimal] = None,
                      max_agg: Option[BigDecimal] = None,
                      min_agg: Option[BigDecimal] = None,
                      count_agg: Option[Long] = None,




                      )
object DruidRecord{
  implicit val druidRecord = Json.reads[DruidRecord]
}

class DruidAPIManager @Inject() (config: Configuration, ws: WSClient, implicit val ec: ExecutionContext){
  val request: WSRequest = ws.url(config.underlying.getString("querier.url"))

  /**
   * Return records after query by SQL
   * @param sqlQuery
   * @return
   */
  def getRecords(sqlQuery: String): Future[List[DruidRecord]] = {
    val data = Json.obj(
      "query" -> sqlQuery
    )
    val response = request.post(data)
    response.map(resp => {
      resp.json.validate[List[DruidRecord]] match{
        case JsSuccess(value, path) =>value
        case JsError(errors) =>
          throw new Exception("Error reading JSON schema from Druid")
      }
    })
  }
}

