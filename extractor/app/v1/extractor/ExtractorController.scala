package v1.extractor

import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class ExtractorController @Inject() (val cc: ControllerComponents, val extractorServiceImpl: ExtractorServiceImpl)
  extends BaseController with I18nSupport {

  implicit val executionContext: ExecutionContext = cc.executionContext
  private val logger = Logger(getClass)

  /**
   * Post handler. Validate the body using ExtractorForm and return a response with serviceIMpl
   * @return
   */
  def post: Action[AnyContent] = Action.async{ implicit request => {
    logger.trace(s"Posting extractor from request with id ${request.id}")
    ExtractorForm.form.bindFromRequest().fold(
      formWithErrors => {
        logger.trace(s"Error validating from request with id ${request.id}")
        Future{BadRequest(ExtractorForm.formatErrorMessage(formWithErrors.errors))}
      },
      extractorData =>{
        logger.trace("Correct validation of extractor input data, now checking sharding config")
        extractorServiceImpl.postExtractor(extractorData)
      }
    )
    }
  }
  /**
   * Get handler. Get extractor info
   * and return a response with serviceIMpl
   * @return
   */
  def get(id: Long): Action[AnyContent] = Action.async{ implicit request => {
    logger.trace(s"Getting extractor with id $id from request ${request.id}")
    extractorServiceImpl.getExtractor(id)
    }
  }

  /**
   * Put handler. Update an existing extractor
   * @param id must be the same as the one spcified in the body
   * @return
   */
  def put(id: Long): Action[AnyContent] = Action.async{ implicit request => {
    logger.trace(s"Updating extractor with id $id from request ${request.id}")
    ExtractorForm.form.bindFromRequest().fold(
      formWithErrors => {
        logger.trace(s"Error validating from request with id ${request.id}")
        Future{BadRequest(ExtractorForm.formatErrorMessage(formWithErrors.errors))}
      },
      extractorData =>{
        logger.trace("Correct validation of extractor input data, now checking sharding config")
        if (extractorData.id == id)
          extractorServiceImpl.updateExtractor(id,extractorData)
        else
          Future{BadRequest(JSONError.format(Json.obj(
            "id" -> s"Url id $id does not match body id ${extractorData.id} fore request ${request.id}"
          )))}
      }
    )
  }
  }

  /**
   * Delete handler. delete permanently an extractor
   * @param id for the extractor
   * @return
   */
  def delete(id: Long): Action[AnyContent] = Action.async{ implicit request => {
    logger.trace(s"Getting extractor with id $id from request ${request.id}")
    extractorServiceImpl.deleteExtractor(id)
    }
  }

  /**
   * start handler. Start an stopped extractor
   * @param id extractor id
   * @return
   */
  def getStart(id: Long): Action[AnyContent] = Action.async{ implicit request => {
    logger.trace(s"Getting extractor with id $id from request ${request.id}")
    extractorServiceImpl.startExtractor(id)
    }
  }

  /**
   * Stop handler. Stop an extractor calling serviceImpl
   * @param id extractor id
   * @return
   */
  def getStop(id: Long): Action[AnyContent] = Action.async{ implicit request => {
    logger.trace(s"Getting extractor with id $id from request ${request.id}")
    extractorServiceImpl.stopExtractor(id)

    }
  }

  override protected def controllerComponents: ControllerComponents = cc
}
