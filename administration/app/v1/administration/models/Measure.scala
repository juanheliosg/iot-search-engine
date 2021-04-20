package v1.administration.models

import play.api.libs.json.{Format, Json}

/**
 * Represents a measure taken by the sensors of the source.
 *
 * @param id
 * @param name name of the measure
 * @param unit unit of the measure expressed in a string
 * @param description
 */
case class Measure(id: Long, name: String, unit: String, description: Option[String])
object Measure{
  implicit val format = Json.format[Measure]
}