package v1.querier

import play.api.libs.json.{JsObject, Json}
import v1.querier.models.{Point, Query, QueryResponse, Series}

import java.time.{LocalDateTime, ZoneId, ZonedDateTime}
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

  /**
   * Returns a query response object from an SQL query
   * @param rawRecords list of raw druid records to be transformed
   */
  def arrangeQuery(rawRecords: Future[List[DruidRecord]])(implicit ec: ExecutionContext): Future[List[QueryResponse]] = {

    rawRecords.map(rawList => rawList.groupBy(_.seriesID)
      .map( f = seriesIdMap => {

        val seriesId = seriesIdMap._1
        val recordList = seriesIdMap._2

        val firstRecord = recordList(0)

        val stats = composeStats(firstRecord)

        val series = recordList.map(point =>
          Point(point.measure, ZonedDateTime.parse(point.__time))
        ).sortWith((first, second) => //sort using unix epoch
          first.timestamp.toEpochSecond < second.timestamp.toEpochSecond
        ).toArray

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
          Series(series),
          stats
        )
      }
    ).toList)

  }

}
