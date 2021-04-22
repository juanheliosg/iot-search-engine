package v1.extractor.models.extractor

import play.api.libs.json.{Format, Json}


/**
 * Schema used for mapping input data to system representation
 *
 * @param sensorIDField External ID field
 * @param timestampField timestamp field
 * @param measures list of measures
 */
case class DataSchema(sensorIDField: String, timestampField: String, measures: List[MeasureField])

object DataSchema{
  /**
   * Compose a system unique identifier for a time series identified by sensor ID, source ID and measure ID
   * @param sensorID
   * @param sourceID
   * @param measureID
   * @return identifier
   */
  def composeUniqueSerieID(sensorID: String, extID: String, measureID: String): String ={
    (sensorID + extID + measureID).replace("\"","")
  }
  implicit val format: Format[DataSchema] = Json.format
}