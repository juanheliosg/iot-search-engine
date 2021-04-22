package v1.extractor.models.extractor.config

import play.api.libs.json._

/**
 * Basic InputConfig for defining the sensor input
 *
 * @param address direction to query by the stream
 * @param httpConfig
 */
case class InputConfig(address: String, httpConfig: Option[HttpInputConfig])

object InputConfig {
  implicit val format: Format[InputConfig] = Json.format
}