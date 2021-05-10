package v1.querier


import play.api.Configuration
import play.api.libs.json.{Format, JsError, JsObject, JsSuccess, Json, OFormat, Reads, Writes}
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
  implicit val druidReader: Format[DruidRecord] = Json.format[DruidRecord]
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
  implicit val druidErrorReader: Reads[DruidError] = Json.reads[DruidError]

}

case class DruidCountField(name: String, value: Long)
object DruidCountField{
  implicit val druidGeneralReads :Reads[DruidCountField] = Json.reads[DruidCountField]
  implicit val druidGeneralWrites :Writes[DruidCountField] = Json.writes[DruidCountField]
}

class DruidAPIManager @Inject() (config: Configuration, ws: WSClient, implicit val ec: ExecutionContext){
  val request: WSRequest = ws.url(config.underlying.getString("querier.druid-url"))
  val datasource = config.underlying.getString("querier.datasource")

  def postGeneralQuery[T](data: JsObject)(implicit rds: Reads[T]): Future[Either[List[T],DruidError]]  ={
    val response: Future[WSResponse] = request.post(data)
    response.map(resp => {
      if(resp.status == 200){
        resp.json.validate[List[T]] match{
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

  /**
   * Return records after query by SQL
   * @param sqlQuery
   * @return
   */

  def getRecords(sqlQuery: String): Future[Either[List[DruidRecord],DruidError]] = {
    val data = Json.obj(
      "query" -> sqlQuery
    )

    postGeneralQuery[DruidRecord](data)(DruidRecord.druidReader)
  }
  def getTags: Future[Either[List[DruidCountField], DruidError]] = {
    val data = Json.obj(
      "query" -> s"SELECT DISTINCT(TAGS), COUNT(*) FROM $datasource"
    )
    postGeneralQuery[DruidCountField](data)(DruidCountField.druidGeneralReads)
  }

  def getNames: Future[Either[List[DruidCountField], DruidError]] = {
    val data = Json.obj(
      "query" -> s"SELECT DISTINCT(name), COUNT(*) FROM $datasource"
    )
    postGeneralQuery[DruidCountField](data)(DruidCountField.druidGeneralReads)
  }
}

