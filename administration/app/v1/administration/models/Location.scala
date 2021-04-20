package v1.administration.models

import play.api.libs.json.{Format, Json}

/**
 * Class representing a location for a source. This may represents the industry or city location where the sensors are isntalled.
 *
 * @param id
 * @param lat     latitude in grades
 * @param long    longitude in grades
 * @param name    name of the location
 * @param address address of hte place
 * @param city    city or poblation where the city is placed
 * @param region  region where the city stays
 * @param country
 */
case class Location(id: Long, lat: Double, long: Double, name: String,
                    address: Option[String], city: Option[String], region: Option[String], country: Option[String])
object Location{
  implicit val format = Json.format[Location]
}