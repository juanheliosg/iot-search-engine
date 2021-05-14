package v1.querier


import play.api.Configuration
import play.api.libs.json._
import play.api.libs.ws._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


case class DruidRecord(
                      seriesID: String,
                      sensorID: String,
                      __time: String = "",
                      address: String,
                      city: String,
                      country: String,
                      description: String,
                      measure: BigDecimal = 0,
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
  implicit val druidReader: Format[DruidRecord] = Json.format[DruidRecord]
}
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
                       host: String = ""
                     )

object DruidError {
  implicit val JSONErrorReader: Reads[DruidError] = Json.reads[DruidError]

}
case class DruidCountField(name: String, value: Long)
object DruidCountField{
  implicit val druidGeneralReads :Reads[DruidCountField] = Json.reads[DruidCountField]
  implicit val druidGeneralWrites :Writes[DruidCountField] = Json.writes[DruidCountField]
}

class DruidAPIManager @Inject() (config: Configuration, ws: WSClient, implicit val ec: ExecutionContext) extends GeneralAPIManager{
  val request: WSRequest = ws.url(config.underlying.getString("querier.druid-url"))
  private val datasource = config.underlying.getString("querier.datasource")



  /**
   * Return records after query by SQL
   * @param sqlQuery
   * @return
   */

  def getRecords(sqlQuery: String): Future[Either[List[DruidRecord],List[JSONError]]] = {
    val data = Json.obj(
      "query" -> sqlQuery
    )
    postGeneralQuery[DruidRecord, DruidError](data, request, JSONError.toErrorList)(DruidRecord.druidReader, ec, DruidError.JSONErrorReader)
  }
  def getTags: Future[Either[List[DruidCountField], List[JSONError]]] = {
    val data = Json.obj(
      "query" -> s"SELECT DISTINCT(TAGS), COUNT(*) FROM $datasource"
    )
    postGeneralQuery[DruidCountField,DruidError](data, request, JSONError.toErrorList)(DruidCountField.druidGeneralReads,ec, DruidError.JSONErrorReader)
  }

  def getNames: Future[Either[List[DruidCountField], List[JSONError]]] = {
    val data = Json.obj(
      "query" -> s"SELECT DISTINCT(name), COUNT(*) FROM $datasource"
    )
    postGeneralQuery[DruidCountField,DruidError](data,request, JSONError.toErrorList)(DruidCountField.druidGeneralReads,ec, DruidError.JSONErrorReader)
  }
}

