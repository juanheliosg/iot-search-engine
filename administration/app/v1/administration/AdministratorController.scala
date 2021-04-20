package v1.administration

import play.api.i18n.I18nSupport
import play.api.mvc.{BaseController, ControllerComponents}
import v1.administration.models.SourceRepository

import javax.inject._
import scala.concurrent.ExecutionContext

@Singleton
class AdministratorController @Inject()(implicit executionContext: ExecutionContext,
                                        val sourceRepository: SourceRepository,
                                        val cc: ControllerComponents) extends BaseController with I18nSupport{



  override protected def controllerComponents: ControllerComponents = cc

}
