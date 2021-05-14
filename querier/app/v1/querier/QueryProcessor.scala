package v1.querier

import v1.querier.models.QueryResponse

import java.time.ZonedDateTime
import scala.concurrent.{ExecutionContext, Future}

object QueryProcessor{

  def composeStats(record: DruidRecord): Seq[(String,String)] = {
    val stats = Array.newBuilder[(String,String)]
    if ( record.avg_agg.nonEmpty )
      stats.addOne(("avg",record.avg_agg.get.toString))
    if ( record.stddev_agg.nonEmpty )
      stats.addOne(("avg",record.stddev_agg.get.toString))
    if ( record.max_agg.nonEmpty )
      stats.addOne(("avg",record.max_agg.get.toString))
    if ( record.min_agg.nonEmpty )
      stats.addOne(("avg",record.min_agg.get.toString))
    if (record.sum_agg.nonEmpty)
      stats.addOne(("avg",record.sum_agg.get.toString))

    stats.result()
  }

  def arrangeQueryResponse(seriesIdMap: (String, List[DruidRecord]), timeseries: Boolean): QueryResponse = {
    val seriesId = seriesIdMap._1
    val recordList = seriesIdMap._2

    val firstRecord = recordList.head

    val stats = composeStats(firstRecord)


    val series = timeseries match {
      case true =>
        recordList.map(point =>
          (point.measure, ZonedDateTime.parse(point.__time)))
          .sortWith((first, second) => //sort using unix epoch
            first._2.toEpochSecond < second._2.toEpochSecond
          ).toArray
      case false =>  Array.empty
    }

    val timestamps =
      if (series.isEmpty)
        Array.empty
      else
        series.map(_._2.toString)

    val values =
        if (series.isEmpty)
          Array.empty
        else
          series.map(_._1)

    QueryResponse(
    seriesId,
    firstRecord.name,
    firstRecord.sensorID,
    firstRecord.description,
    firstRecord.city,
    firstRecord.region,
    firstRecord.country,
    firstRecord.address,
    firstRecord.sampling_unit,
    firstRecord.sampling_freq,
    firstRecord.measure_name,
    firstRecord.unit,
    firstRecord.measure_desc,
    firstRecord.tags,
    firstRecord.coordinates,
    timestamps,
    values,
    stats
    )
  }

  /**
   * Returns a query response object from an SQL query
   * @param rawRecords list of raw druid records to be transformed
   */
  def arrangeQuery(rawRecords: List[DruidRecord],timeseries: Boolean)(implicit ec: ExecutionContext): List[QueryResponse] = {
    rawRecords.groupBy(_.seriesID).map( f = seriesIdMap => {
      arrangeQueryResponse(seriesIdMap,timeseries)
    }
    ).toList
  }

  /**
   * Returns a query response object from an SQL query
   * @param rawRecords list of raw druid records to be transformed
   */
  def arrangeMapQueryResponse(rawRecords: List[DruidRecord], timeseries: Boolean)(implicit ec: ExecutionContext): Map[String,QueryResponse] = {
    val seriesIdResponse = collection.mutable.Map[String,QueryResponse]()
    rawRecords.groupBy(_.seriesID).foreach( f = seriesIdMap => {
      seriesIdResponse +=(seriesIdMap._1 -> arrangeQueryResponse(seriesIdMap,timeseries))
    })
    seriesIdResponse.toMap
  }

}
