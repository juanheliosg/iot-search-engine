package v1.querier

import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class QuerierController @Inject() (val cc: ControllerComponents)
  extends BaseController with I18nSupport{
  private val logger = Logger(getClass)
  implicit val executionContext: ExecutionContext = cc.executionContext

  def postQuery: Action[AnyContent] = Action.async{ implicit request => {

    logger.trace(s"Posting extractor from request with id ${request.id}")
    QueryForm.form.bindFromRequest().fold(
      formWithErrors => {
        Future{
          BadRequest(JSONError.format(formWithErrors.errors))
        }
      },
      query => {
        Future{Ok("")}
      }
    )
  }
  }

  override protected def controllerComponents: ControllerComponents = cc

}
