package v1.extractor.models.extractor.config

import play.api.libs.json.{Format, Json}

/**
 * Http input config
 *
 * @param jsonPath string with a jsonPath notation for specifying the array with the sensor measures
 * @param freq     frequency of retrieval in milliseconds.
 */
case class HttpInputConfig(jsonPath: String, freq: Long)

object HttpInputConfig {
  implicit val format: Format[HttpInputConfig] = Json.format
}