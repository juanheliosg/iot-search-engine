package v1.administration.models

import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import v1.administration.models.TimeUnit.TimeUnit

import java.util.Date
import javax.inject.Inject
import scala.concurrent.ExecutionContext


/**
 * A source represents a collection of entities in the real world
 * who are emitting data through sensors. All the common properties of
 * these entities are represented inside this object.
 *
 * @param id unique identifier
 * @param name unique name for the source
 * @param extractorID optional extractor ID representing an extractor job
 * @param description summary of the collection and of entities
 * @param sourceType tag-like classification of sources
 * @param measures list of measurements that the collection of entites make
 * @param sampling information about sampling
 * @param localization spatial information about the collection
 * @param url optional url for getting more information about the source
 * @param installationDate optional date for the collection installation.
 */
case class Source(id: Long,
                  name: String,
                  extractorID: Option[Long],
                  description: String,
                  sourceType : SourceType,
                  measures: Seq[Measure],
                  sampling: Sample,
                  localization: Location,
                  url: Option[String],
                  installationDate: Option[Date])













