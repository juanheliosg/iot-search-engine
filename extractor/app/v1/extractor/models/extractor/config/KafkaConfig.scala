package v1.extractor.models.extractor.config

import play.api.libs.json.{Format, Json}

/**
 * Object encapsulating a basic Kafka configuratio
 *
 * @param topic
 * @param server
 */
case class KafkaConfig(topic: String, server: String)
object KafkaConfig {
  implicit val format: Format[KafkaConfig] = Json.format
}