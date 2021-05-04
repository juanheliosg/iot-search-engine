package v1.querier

import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}
import play.api.libs.json._
import v1.querier.models.{Query, QueryType}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class QuerierController @Inject() (val cc: ControllerComponents, val druidApi: DruidAPIManager)
  extends BaseController with I18nSupport{
  private val logger = Logger(getClass)
  implicit val executionContext: ExecutionContext = cc.executionContext

  def getRawRecords(query: Query) = {
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

  def postQuery: Action[AnyContent] = Action.async{ implicit request => {

    logger.trace(s"Posting extractor from request with id ${request.id}")
    QueryForm.form.bindFromRequest().fold(
      formWithErrors => {
        Future{
          BadRequest(JSONError.format(formWithErrors.errors))
        }
      },
      success = query => {
        query.queryType match {
          case QueryType.complex =>
            val rawRecords = getRawRecords(query)
            Future {
              Ok("Ok")
            }
          case _ =>
            val rawResponseFuture = getRawRecords(query)
            rawResponseFuture.flatMap{
              case Left(rawRecords) =>
                val queryResponseList = QueryProcessor.arrangeQuery(Future{
                  rawRecords
                })
                queryResponseList.map(list => {
                  val finalList = list.take(query.limit)
                  Ok(Json.obj(
                    "items" -> finalList.size,
                    "series" -> finalList.map(_.toJson())
                  ))
                }
                )

              case Right(error) =>
                Future {
                  BadRequest(JSONError.format(error))
                }
            }
        }
      }
    )
  }
  }

  override protected def controllerComponents: ControllerComponents = cc

}
