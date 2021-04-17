package v1.extractor
import play.api.libs.json._
import v1.extractor.ExtractorType.ExtractorType

/**
 * This file consist of the main models used by actors and extractors
 */


/**
 * Represents a sensor measure
 * @param name: represent sensor name measure
 * @param field: mapping field
 * @param measureID: id of the measure
 */
case class Measure(name: String, field: String, measureID: Long)
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
case class DataSchema(sourceID: Long, sensorIDField: String, timestampField: String, measures: List[Measure]){
  /**
   * Compose a system unique identifier for a time series identified by sensor ID, source ID and measure ID
   * @param sensorID
   * @param sourceID
   * @param measureID
   * @return identifier
   */
  def composeUniqueSerieID(sensorID: String, sourceID: String, measureID: String): String ={
    (sensorID + sourceID + measureID).replace("\"","")
  }
}
object DataSchema{
  implicit val format: Format[DataSchema] = Json.format
}

case class KafkaConfig(topic: String, server: String)
object KafkaConfig{
  implicit val format: Format[KafkaConfig] = Json.format
}

case class InputConfig(address: String, httpConfig: Option[HttpInputConfig]);
object InputConfig{
  implicit val format: Format[InputConfig] = Json.format
}
case class HttpInputConfig(jsonPath: String, freq: Long)
object HttpInputConfig{
  implicit val format: Format[HttpInputConfig] = Json.format
}

case class IOConfig(inputConfig: InputConfig, kafkaConfig: KafkaConfig)
object IOConfig{
  implicit val format: Format[IOConfig] = Json.format
}

/**
 *
 * @param id unique extractorID
 * @param schema
 * @param ioConfig
 */
case class ExtractorState(schema: DataSchema, config: IOConfig, extractorType: ExtractorType)


/**
 * Class for serializing responses containing extractor status
 * @param id extractor entity id
 * @param status extractor status
 */
case class ExtractorStatusResponse(id: Long,status: String)
object ExtractorStatusResponse{
  implicit val format: Format[ExtractorStatusResponse] = Json.format
}

/**
 * Class for serializing responses containing extractor data
 * @param id extractor entity id
 * @param status extractor status
 * @param data data containing schema,
 */
case class ExtractorGetResponse(id: Long, status: String, extType: String , dataSchema: DataSchema, ioConfig: IOConfig )
object ExtractorGetResponse{
  implicit val format: Format[ExtractorGetResponse] = Json.format
}
