package v1.querier.models

import play.api.libs.json.{Format, Json}

import java.time.{LocalDateTime, ZonedDateTime}

/**
 * Object representing a series
 * @param series ordered by time array containing a tuple of timestamp and measure
 */
case class Series(series: Array[Point])
object Series{
  implicit val format: Format[Series] = Json.format[Series]
}

case class Point(value: BigDecimal, timestamp: ZonedDateTime)
object Point{
  implicit val format: Format[Point] = Json.format[Point]
}