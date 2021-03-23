package v1.extractor
import play.api.libs.json._

/**
 * Enum for the different type of extractors avalaible in the system
 * Current avalaible extractors are:
 *
 * http -> Uses http protocol to extract data in JSON format from an endpoint
 * rw_sym -> Simulate a random walk process
 */
object ExtractorType extends Enumeration{
  type OrderType = Value
  val Http = Value("http")
  val RANDOM_WALK_SYM = Value("rw_sym")

  /**
   * Check if string is inside the enum
   * @param s
   * @return
   */
  def isExtractorType(s: String) = values.exists(_.toString == s)

  /**
   * Enables matching against all ExtractorType.values
   * source: https://stackoverflow.com/questions/3407032/comparing-string-and-enumeration
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

/**
 * Represents a sensor measure
 * @param name: represent sensor name measure
 * @param field: mapping field
 */
case class Measure(name: String, field: String)
object Measure{
  implicit val format: Format[Measure] = Json.format
}

/**
 * Schema used for mapping input data to system representation
 * @param sourceID Data origin ID
 * @param sensorIDField External ID field
 * @param timestampField timestamp field
 * @param measures list of measures
 */
case class DataSchema(sourceID: Long, sensorIDField: String, timestampField: String, measures: List[Measure])
object DataSchema{
  implicit val format: Format[DataSchema] = Json.format
}

/**
 * Configuration for the extractor includ
 * @param address uri direction for retrieving sensor data
 * @param jsonPath optional jsonPATH for selecting
 * @param freq optional retrieving frequency
 */
case class IOConfig(address: String, jsonPath: Option[String], freq: Option[Long])
object IOConfig{
  implicit val format: Format[IOConfig] = Json.format
}

/**
 *
 * @param id unique extractorID
 * @param schema
 * @param ioConfig
 */
case class Extractor(id: Long,
                     schema: DataSchema,
                     ioConfig: IOConfig)

object Extractor{
  implicit val format: Format[Extractor] = Json.format

}

class ExtractorResourceHandler {

}
