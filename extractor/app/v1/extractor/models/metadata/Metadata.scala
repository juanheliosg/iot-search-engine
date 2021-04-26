package v1.extractor.models.metadata

import play.api.libs.json.{Format, Json}
import spray.json.{JsArray, JsNumber, JsString, JsValue}

import java.util.Date


/**
 * A metadata class representing general information
 * of the sensors being collected. All the common properties of
 * these entities are represented inside this object.
 *
 * @param name unique name for the source
 * @param description summary of the collection and of entities
 * @param tags tag-like classification of sources
 * @param sampling information about sampling
 * @param localization spatial information about the collection
 * @param url optional url for getting more information about the source
 */
case class Metadata(name: String,
                  description: Option[String] = None,
                  tags: Seq[String],
                  sampling: Sample,
                  localization: Location,
                  url: Option[String] = None){

  def metadataMap(): Map[String,JsValue] =
    Map(
      "name" -> JsString(name),
      "description" -> JsString(description.getOrElse("")),
      "tags" -> JsArray(tags.map(JsString(_)).toVector),
      "sampling_unit" -> JsString(sampling.unit),
      "sampling_freq" -> JsNumber(sampling.freq),
      "address" -> JsString(localization.address.getOrElse("")),
      "city" -> JsString(localization.city.getOrElse("")),
      "region" -> JsString(localization.region.getOrElse("")),
      "country" -> JsString(localization.country.getOrElse("")),
    )

}


object Metadata{
  implicit val format :Format[Metadata] = Json.format

}





