package v1.administration.models

import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.Cursor
import reactivemongo.api.bson.BSONDocument
import reactivemongo.api.commands.WriteResult
import reactivemongo.play.json.compat.ExtendedJsonConverters.fromValue
import reactivemongo.play.json.compat.bson2json.fromDocumentWriter

import java.util.Date
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


/**
 * A source represents a collection of entities in the real world
 * who are emitting data through sensors. All the common properties of
 * these entities are represented inside this object.
 *
 * @param _id unique identifier
 * @param name unique name for the source
 * @param extractorID optional extractor ID representing an extractor job
 * @param description summary of the collection and of entities
 * @param tags tag-like classification of sources
 * @param measures list of measurements that the collection of entites make
 * @param sampling information about sampling
 * @param localization spatial information about the collection
 * @param url optional url for getting more information about the source
 * @param installationDate optional date for the collection installation.
 */
case class Source(_id: Long,
                  name: String,
                  extractorID: Option[Long],
                  description: Option[String],
                  measures: Seq[Measure],
                  tags: Seq[String],
                  sampling: Sample,
                  localization: Location,
                  url: Option[String],
                  installationDate: Option[Date])


object Source{
  implicit val format = Json.format[Source]
}

/**
 * Manage database operations of a Source object
 * @param ec
 * @param reactiveMongoApi
 */
class SourceRepository @Inject()(implicit  ec: ExecutionContext, reactiveMongoApi: ReactiveMongoApi){
  import reactivemongo.play.json._
  import collection._

  private def sourceCollection: Future[JSONCollection] =
    reactiveMongoApi.database.map(_.collection("sources"))

  /**
   * Get all sources in the db.
   * @param limit limit for the number of sources to return
   * @return
   */
  def getAll( limit: Int = 1000): Future[Seq[Source]] =
    sourceCollection.flatMap(_.find(BSONDocument.empty, Option.empty[Source])
      .cursor[Source]().collect[Seq](limit, Cursor.FailOnError[Seq[Source]]()))

  /**
   * Find source with the given index
   * @param id
   * @return
   */
  def findSource(id: Long): Future[Option[Source]] =
    sourceCollection.flatMap(_.find(BSONDocument("_id" -> id), Option.empty[Source]).one[Source])

  /**
   * Create in the db a new source
   *
   * @param source to be created
   * @return
   */
  def create(source: Source): Future[WriteResult] = {
    sourceCollection.flatMap(_.insert(ordered=false).one(source))
  }

  /**
   * Update a source by the given ID
   * @param id
   * @param source
   * @return
   */
  def update(id: Long, source: Source): Future[WriteResult] = {
    sourceCollection.flatMap(_.update(ordered = false).one(BSONDocument("_id"->id),source))
  }

  /**
   * delete a source identified by the id
   * @param id
   * @return WriteResult
   */
  def delete(id: Long): Future[WriteResult] = {
    sourceCollection.flatMap(
      _.delete().one(BSONDocument("_id" -> id),Some(1))
    )
  }
}




