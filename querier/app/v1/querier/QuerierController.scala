package v1.querier

import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents, Result}
import play.api.libs.json._
import v1.querier.models.{Query, QueryResponse, QueryType, Subsequence}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class QuerierController @Inject() (val cc: ControllerComponents, val druidApi: DruidAPIManager, val tsAPI: TsAnalysisManager)
  extends BaseController with I18nSupport{
  private val logger = Logger(getClass)
  private val maxSubseqQuery = 1000
  implicit val executionContext: ExecutionContext = cc.executionContext

  def getQueryResponses(query: Query): Future[Either[Future[Seq[QueryResponse]], List[JSONError]]] = {
    query.queryType match {
      case QueryType.simple =>
        druidApi.getRecordsWithStream(query.composeBasicQuery)
      case QueryType.aggregation =>
        druidApi.getRecordsWithStream(query.composeAggregationQuery)
      case QueryType.complex =>
        val sqlQuery = if (query.aggregationFilter.nonEmpty
              && query.aggregationFilter.get.nonEmpty){
          query.composeAggregationQuery
        }else {
          query.composeBasicQuery
        }
        druidApi.getRecordsWithStream(sqlQuery)
    }
  }

  /**
   * Make a complex query which includs druid queries and tsanalysis queries
   * @param query
   * @return
   */
  def complexQuery(query: Query): Future[Result] = {
    if (query.subseQuery.nonEmpty){
      val subseQuery = query.subseQuery.get
      val futureResponsesSeq = getQueryResponses(query)
      futureResponsesSeq.flatMap{
        case Left(futureResponsesSeq) =>
          futureResponsesSeq.flatMap(queryResponseList => {
              val queryResponseMap = queryResponseList.map(qr => (qr.seriesId,qr)).toMap
              val subseqResults = tsAPI.searchSubsequence(queryResponseMap.toList, subseQuery.subsequence)
              subseqResults.map{
                case Left(subseq) =>
                  subseq.foreach(subse => {
                    queryResponseMap(subse.series_id).subsequences += (Subsequence(subse.ed,subse.start))
                  })
                  val finalList = queryResponseMap.values.toList.sortWith{ (first, second) => {
                    if (first.subsequences.nonEmpty && second.subsequences.nonEmpty){
                      first.subsequences.maxBy(_.ed).ed > second.subsequences.maxBy(_.ed).ed
                    }
                    else if (first.subsequences.nonEmpty){
                      true
                    }
                    else{
                      false
                    }

                  }
                  }.take(query.limit)
                  Ok(Json.obj(
                    "items" -> finalList.size,
                    "series" -> finalList.map(_.toJson)
                  ))
                case Right(error) =>
                  BadRequest(JSONError.format(error))
              }
          })
        case Right(error) =>
          Future{BadRequest(JSONError.format(error))}
      }
    }else{
      Future{BadRequest(
        JSONError.format(
          List(JSONError("Empty subseqQuery", "You must provide a subseqQuery when "))
        ))}
    }
  }

  def postQuery: Action[AnyContent] = Action.async{ implicit request => {

    logger.trace(s"Posting query for request with id ${request.id}")
    QueryForm.form.bindFromRequest().fold(
      formWithErrors => {
        Future{
          BadRequest(JSONError.formatForm(formWithErrors.errors))
        }
      },
      success = query => {
        query.queryType match {
          case QueryType.complex =>
            try{
              complexQuery(query)
            }catch{
              case _ =>
                Future{InternalServerError(
                  JSONError.format(
                    List(JSONError("Error processing query", "Error processing complex query. Try to make a query with less sensors"))
                  ))}
            }
          case _ =>
            val queryResponseFuture = getQueryResponses(query)
            queryResponseFuture.flatMap({
              case Left(queryResponseListFuture) =>
                queryResponseListFuture.map( queryResponseList => {
                  val finalList = queryResponseList.take(query.limit)
                  Ok(Json.obj(
                    "items" -> finalList.size,
                    "series" -> finalList.map(_.toJson)
                  ))
                }
                )
              case Right(error) =>
                Future{BadRequest(JSONError.format(error))}
            })
        }
      }
    )
  }
  }

  private def getFieldCount(field: String): Future[Result] = {
    druidApi.getCountField(field).flatMap{
      case Left(rawTags) =>
        Future{
          Ok(Json.obj(
            "items" -> rawTags.size,
            "count"-> rawTags))
        }
      case Right(error) =>
        Future {
          BadRequest(JSONError.format(error))
        }
    }
  }
  def getTags: Action[AnyContent] = Action.async{ implicit request => {
    logger.trace(s"Getting tags for request with id ${request.id}")
    getFieldCount("tags")
  }
  }
  def getNames: Action[AnyContent] = Action.async{ implicit request => {
    logger.trace(s"Getting measure names from request with id ${request.id}")
    getFieldCount("name")
  }
  }
  def getMeasuresName: Action[AnyContent] = Action.async{ implicit request => {
    logger.trace(s"Getting measure names from request with id ${request.id}")
    getFieldCount("measure_name")
  }
  }
  def getMeasuresUnit: Action[AnyContent] = Action.async{ implicit request => {
    logger.trace(s"Getting measures units from request with id ${request.id}")
    getFieldCount("unit")
  }
  }

  def getSamplingUnits: Action[AnyContent] = Action.async{ implicit request => {
    logger.trace(s"Getting sampling_units from request with id ${request.id}")
    getFieldCount("sampling_unit")
  }
  }
  def getCities: Action[AnyContent] = Action.async{ implicit request => {
    logger.trace(s"Getting cities from request with id ${request.id}")
    getFieldCount("city")
  }
  }
  def getRegions: Action[AnyContent] = Action.async{ implicit request => {
    logger.trace(s"Getting regions from request with id ${request.id}")
    getFieldCount("region")
  }
  }
  def getCountries: Action[AnyContent] = Action.async{ implicit request => {
    logger.trace(s"Getting countries from request with id ${request.id}")
    getFieldCount("country")
  }
  }

  override protected def controllerComponents: ControllerComponents = cc

}
