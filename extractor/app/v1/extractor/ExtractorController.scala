package v1.extractor

import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.libs.json.JsObject
import play.api.mvc._

import javax.inject.{Inject, Singleton}


@Singleton
class ExtractorController @Inject() (val cc: ControllerComponents) extends BaseController with I18nSupport{

  private val logger = Logger(getClass)

  def post: Action[AnyContent] = Action{ implicit request => {
    logger.trace("posting extractor:")
    ExtractorForm.form.bindFromRequest().fold(
      formWithErrors => {
        logger.trace(s"Error validating from request with id ${request.id}")
        BadRequest(ExtractorForm.formatErrorMessage(formWithErrors.errors))
      },
      extractorData => {
        logger.trace("Correct validation of extractor input data")
        Created("Okey makina")
    }
    )
    // Validar el formulario con bind
    // Comprobar que no existe ningún extractor más para la misma fuente del mismo tipo. Hacer esto después del BD
    // Crear el extractor en la BD (seguramente MongoDB). Lll
    // Mandar mensaje a Actor jefe supremo para la creación  del actor de extracción
    // Mandar mensaje de vuelta indicando que va bien

  }
  }

  override protected def controllerComponents: ControllerComponents = cc
}
