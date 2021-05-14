package v1.querier

import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents, Result}
import play.api.libs.json._
import v1.querier.models.{Query, QueryType, Subsequence}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class QuerierController @Inject() (val cc: ControllerComponents, val druidApi: DruidAPIManager, val tsAPI: TsAnalysisManager)
  extends BaseController with I18nSupport{
  private val logger = Logger(getClass)
  private val maxSubseqQuery = 1000
  implicit val executionContext: ExecutionContext = cc.executionContext

  def getRawRecords(query: Query): Future[Either[List[DruidRecord], List[JSONError]]] = {
    query.queryType match {
      case QueryType.simple =>
        druidApi.getRecords(query.composeBasicQuery)
      case QueryType.aggregation =>
        druidApi.getRecords(query.composeAggregationQuery)
      case QueryType.complex =>
        val sqlQuery = if (query.aggregationFilter.nonEmpty
              && query.aggregationFilter.get.nonEmpty){
          query.composeAggregationQuery
        }else {
          query.composeBasicQuery
        }
        druidApi.getRecords(sqlQuery)
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
      val rawRecords = getRawRecords(query)
      rawRecords.flatMap{
        case Left(rawRecords) =>
          val queryResponseMap = QueryProcessor.arrangeMapQueryResponse(rawRecords,query.timeseries)
          val subseqResults = tsAPI.searchSubsequence(queryResponseMap.toList,subseQuery.subsequence)
          val result = subseqResults.map{
            case Left(subseq) =>
              subseq.foreach(subse => {
                queryResponseMap(subse.series_id)
                  .subsequences.+:(Subsequence(subse.ed,subse.start))
              })
              val finalList = queryResponseMap.values.toList.sortWith{ (first, second) => {
                first.subsequences.maxBy(_.ed).ed > second.subsequences.maxBy(_.ed).ed
              }
              }.take(query.limit)
              Ok(Json.obj(
                "items" -> finalList.size,
                "series" -> finalList.map(_.toJson())
              ))
            case Right(error) =>
              BadRequest(JSONError.format(error))
          }
          result
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
          BadRequest(JSONError.format(formWithErrors.errors))
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
            val rawResponseFuture = getRawRecords(query)
            rawResponseFuture.map{
              case Left(rawRecords) =>
                val queryResponseList = QueryProcessor.arrangeQuery(rawRecords, query.timeseries)
                val finalList = queryResponseList.take(query.limit)
                Ok(Json.obj(
                  "items" -> finalList.size,
                  "series" -> finalList.map(_.toJson())
                ))
              case Right(error) =>
                BadRequest(JSONError.format(error))
            }
        }
      }
    )
  }
  }
  def getTags: Action[AnyContent] = Action.async{ implicit request => {
    logger.trace(s"Getting tags for request with id ${request.id}")
    druidApi.getTags.flatMap{
      case Left(rawTags) =>
        Future{
          Ok(Json.arr(rawTags))
        }
      case Right(error) =>
        Future {
          BadRequest(JSONError.format(error))
        }
    }
  }
  }
  def getNames: Action[AnyContent] = Action.async{ implicit request => {
    logger.trace(s"Getting names from request with id ${request.id}")
    druidApi.getNames.flatMap{
      case Left(rawNames) =>
        Future{
          Ok(Json.arr(rawNames))
        }
      case Right(error) =>
        Future {
          BadRequest(JSONError.format(error))
        }
    }
  }
  }

  override protected def controllerComponents: ControllerComponents = cc

}
