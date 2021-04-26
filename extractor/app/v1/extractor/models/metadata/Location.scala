package v1.extractor.models.metadata

import play.api.libs.json.Json

/**
 * Class representing a location for a source. This may represents the industry or city location where the sensors are isntalled.
 *
 * @param lat     latitude in grades
 * @param long    longitude in grades
 * @param name    name of the location
 * @param address address of hte place
 * @param city    city or poblation where the city is placed
 * @param region  region where the city stays
 * @param country
 */
case class Location(name: String,
                    address: Option[String] = None,
                    city: Option[String] = None,
                    region: Option[String] = None,
                    country: Option[String] = None)

object Location{
  implicit val format = Json.format[Location]
}