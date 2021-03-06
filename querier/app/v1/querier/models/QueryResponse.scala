package v1.querier.models

import play.api.libs.json._

import scala.collection.mutable

/**
 * Subsequence relative to the
 * @param ed euclidean distance to the query subsequence
 * @param start start of the subsequence in the series array
 */
case class Subsequence(ed: BigDecimal, start: Long){

}

object Subsequence{
  implicit val format: Format[Subsequence] = Json.format[Subsequence]
  implicit val order: Ordering[Subsequence] = Ordering.by{ subseq: Subsequence => subseq.ed}
}

/**
 * Response representing a serie which belongs to a sensor
 *
 * @param seriesId unique identifier of the serie
 * @param sourceName name of the source that is capturing this serie
 * @param sensorId sensor that is taking the series measures
 * @param description description of the source
 * @param city city where the sensor is
 * @param region where the sensor is
 * @param country country where the sensor is
 * @param address address for low granularity position
 * @param samplingUnit time unit of the sampling (seconds, minutes)
 * @param samplingFreq freq for sampling
 * @param measureName name of the measure being taken by the sensor
 * @param measureDescr description of the measures
 * @param tags list tags related to the sensor
 * @param coordinates coordinates where the sensor is placed
 * @param timestamps list of timestamps same size as values
 * @param values 
 * @param stats statistical information about the serie (average, standard deviation...)
 * @param subsequences list of subsequences which are significant to the query
 */

case class QueryResponse(seriesId: String,
                          sourceName: String,
                          sensorId: String,
                          description: String,
                          city: String,
                          region: String,
                          country: String,
                          address: String,
                          samplingUnit: String,
                          samplingFreq: String,
                          measureName: String,
                          measureUnit: String,
                          measureDescr: String,
                          tags: Set[String],
                          lat: String,
                          long: String,
                          timestamps: Seq[String],
                          values: Seq[BigDecimal],
                          stats: Seq[(String, String)],
                          subsequences: mutable.TreeSet[Subsequence] = mutable.TreeSet.empty
                        ){

  def toJson: JsObject = {
    Json.obj(
      "seriesId" -> seriesId,
      "sourceName" -> sourceName,
      "sensorId" -> sensorId,
      "description" -> description,
      "city" -> city,
      "region" -> region,
      "country" -> country,
      "address" -> address,
      "samplingUnit" -> samplingUnit,
      "samplingFreq" -> samplingFreq,
      "measureName" -> measureName,
      "measureUnit" -> measureUnit,
      "measureDescr" -> measureDescr,
      "tags" -> tags,
      "lat" -> lat,
      "long" -> long,
      "timestamps" -> timestamps,
      "values" -> values,
      "stats" -> stats.map( stat => Json.obj(
        "name" -> stat._1,
        "value" -> stat._2
      )),
      "subsequences" -> subsequences.toList.map( sub => {
        Json.obj(
          "ed" -> sub.ed,
          "start"-> sub.start,
        )
      })
    )
  }
}

object QueryResponse{
  implicit val format: Format[QueryResponse] = Json.format[QueryResponse]
}

