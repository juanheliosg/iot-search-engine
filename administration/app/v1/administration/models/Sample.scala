package v1.administration.models

import play.api.libs.json.{Format, Json}
import v1.administration.models.TimeUnit.TimeUnit

/**
 * Represent the sampling intervals of a source.
 * if freq=3 and unit = SECONDS sampling intervals are every 3 seconds
 *
 * @param id
 * @param freq frequency of sampling
 * @param unit unit of sampling
 */
case class Sample(id: Long, freq: Long, unit: TimeUnit)

/**
 * Enum representing different time units for sampling intervals
 */
object TimeUnit extends Enumeration{
  type TimeUnit = Value
  val MILLISECONDS = Value("milliseconds")
  val SECONDS = Value("seconds")
  val MINUTE = Value("minute")
  val HOUR = Value("hour")
  val DAY = Value("day")
  val WEEK = Value("week")
  val MONTH = Value("month")
  val YEAR = Value("year")

  /**
   * Check if string is inside the enum
   *
   * @param s
   * @return
   */
  def isSourceType(s: String) = values.exists(_.toString == s)

  /**
   * Enables matching against all ExtractorType.values
   * source: https://stackoverflow.com/questions/3407032/comparing-string-and-enumeration
   *
   * @param s
   * @return
   */
  def unapply(s: String): Option[Value] =
    values.find(s == _.toString)

  /**
   * Trait for doing matching pattern in enum
   * source: https://stackoverflow.com/questions/3407032/comparing-string-and-enumeration
   */
  trait Matching {
    // enables matching against a particular Role.Value
    def unapply(s: String): Boolean =
      (s == toString)
  }
}
object Sample{
  implicit val format = Json.format[Sample]
}