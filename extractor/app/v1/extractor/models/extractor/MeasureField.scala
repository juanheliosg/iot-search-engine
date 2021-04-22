package v1.extractor.models.extractor

import play.api.libs.json.{Format, Json}

object MeasureField {
  implicit val format: Format[MeasureField] = Json.format
}

/**
 * Represents a sensor measure
 *
 * @param name      : represent sensor name measure
 * @param field     : mapping field
 * @param unit  measure unit
 * @param description optional description about the measure.
 */
case class MeasureField(name: String, field: String, unit: String, description: Option[String]=None)