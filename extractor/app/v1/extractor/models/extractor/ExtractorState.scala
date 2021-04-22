package v1.extractor.models.extractor

import play.api.libs.json._
import v1.extractor.ExtractorType.ExtractorType
import v1.extractor.models.metadata.Metadata


/**
 * Represents the internal state of an Akka actor
 * @param schema mapping schema from sensor fields to internal representation
 * @param config input and kafka configuration  used
 * @param extractorType extractor type
 * @param metadata additional common information about the sensors being extracted.
 */
case class ExtractorState(schema: DataSchema, config: IOConfig, extractorType: ExtractorType, metadata: Metadata )


/**
 * Class for serializing responses containing extractor status
 * @param id extractor entity id
 * @param status extractor status
 */
case class ExtractorStatusResponse(id: String,status: String)
object ExtractorStatusResponse{
  implicit val format: Format[ExtractorStatusResponse] = Json.format
}

/**
 * Class for serializing responses containing extractor data
 * @param id extractor entity id
 * @param status extractor status
 * @param data data containing schema,
 */
case class ExtractorGetResponse(id: String, status: String, extType: String , dataSchema: DataSchema, ioConfig: IOConfig, metadata: Metadata )
object ExtractorGetResponse{
  implicit val format: Format[ExtractorGetResponse] = Json.format
}
