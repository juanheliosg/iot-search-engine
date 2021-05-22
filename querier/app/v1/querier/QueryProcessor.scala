package v1.querier

import v1.querier.models.{QueryResponse, Subsequence}

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import scala.collection.mutable
import scala.concurrent.ExecutionContext

trait QueryProcessor{

  case class Measure(point: BigDecimal, ts: ZonedDateTime)
  object Measure{
    implicit val order = Ordering.by{measure: Measure => measure.ts.toEpochSecond}
  }
  case class IntermediateResponse(seriesId: String,
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
                           series: mutable.TreeSet[Measure],
                           stats: Seq[(String, String)],
                           subsequences: mutable.Seq[Subsequence] = mutable.Seq.empty
                          ){
    def toQueryResponse: QueryResponse = {
      QueryResponse(seriesId = seriesId,
        sourceName = sourceName,
        sensorId = sensorId,
        description = description,
        city = city,
        region = region,
        country = country,
        address = address,
        samplingUnit = samplingUnit,
        samplingFreq = samplingFreq,
        measureName = measureName,
        measureUnit = measureUnit,
        measureDescr = measureDescr,
        tags = tags,
        lat = lat,
        long = long,
        timestamps = series.toList.map(_.ts.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)),
        values = series.toList.map(_.point),
        stats = stats,
      )
    }
  }

  def composeStats(record: DruidRecord): Seq[(String,String)] = {
    val stats = Array.newBuilder[(String,String)]
    if ( record.avg_agg.nonEmpty )
      stats.addOne(("avg",record.avg_agg.get.toString))
    if ( record.stddev_agg.nonEmpty )
      stats.addOne(("stddev",record.stddev_agg.get.toString))
    if ( record.max_agg.nonEmpty )
      stats.addOne(("max",record.max_agg.get.toString))
    if ( record.min_agg.nonEmpty )
      stats.addOne(("min",record.min_agg.get.toString))
    if (record.sum_agg.nonEmpty)
      stats.addOne(("sum",record.sum_agg.get.toString))
    stats.result()
  }

  def getIntermediateQueryResponse(dr: DruidRecord): IntermediateResponse = {
    IntermediateResponse(
      dr.seriesID,
      dr.name,
      dr.sensorID,
      dr.description,
      dr.city,
      dr.region,
      dr.country,
      dr.address,
      dr.sampling_unit,
      dr.sampling_freq.toString,
      dr.measure_name,
      dr.unit,
      dr.measure_desc,
      dr.tags.toSet,
      dr.lat,
      dr.long,
      (dr.measure.nonEmpty, dr.__time.nonEmpty) match{
        case (true,true) =>
          mutable.TreeSet(Measure(dr.measure.get, ZonedDateTime.parse(dr.__time.get,DateTimeFormatter.ISO_ZONED_DATE_TIME)))
        case (_,_) =>
          mutable.TreeSet.empty
      },
      composeStats(dr)
    )
  }

  def reduceQueryResponseStream(finalQR: IntermediateResponse, newQR: IntermediateResponse): IntermediateResponse = {
    //Meter timestamps y measures en el mimso lao y ordenarlas
    val concatSeries = finalQR.series ++ newQR.series
    val concatTags = finalQR.tags ++ newQR.tags
    IntermediateResponse(
      finalQR.seriesId,
      finalQR.sourceName,
      finalQR.sensorId,
      finalQR.description,
      finalQR.city,
      finalQR.region,
      finalQR.country,
      finalQR.address,
      finalQR.samplingUnit,
      finalQR.samplingFreq,
      finalQR.measureName,
      finalQR.measureUnit,
      finalQR.measureDescr,
      concatTags,
      finalQR.lat,
      finalQR.long,
      concatSeries,
      finalQR.stats)
  }


}
