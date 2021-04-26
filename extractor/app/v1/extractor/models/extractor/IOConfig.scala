package v1.extractor.models.extractor

import v1.extractor.models.extractor.config.{InputConfig, KafkaConfig}
import play.api.libs.json._
/**
 * IO Config configuration
 *
 * @param inputConfig
 * @param kafkaConfig
 */
case class IOConfig(inputConfig: InputConfig, kafkaConfig: KafkaConfig)

object IOConfig {
  implicit val format: Format[IOConfig] = Json.format
}