package v1.querier

import play.api.data.FormError
import play.api.libs.json.{JsArray, JsValue, Json}

case class JSONError(error: String, errorMessage: String){

}

object JSONError{
  def toErrorList(druidError: DruidError): List[JSONError] = {
    List(JSONError(druidError.error, druidError.errorMessage))
  }
  def apply(tsAnalysisError: TsAnalysisError): JSONError ={
    JSONError(tsAnalysisError.`type`,tsAnalysisError.msg)
  }
  def toErrorList(tsAnalysisListError: TsAnalysisListError): List[JSONError] = {
    tsAnalysisListError.detail.map(er => JSONError(er)).toList
  }

  def format(errors: Seq[JSONError]): JsValue = {
    val errorsFormat = errors.map( err =>
        Json.obj(
          "error" -> err.error,
          "errorMessage" -> err.errorMessage
        )
    )
    Json.toJson(errorsFormat)

  }

  def formatForm(errors: Seq[FormError]): JsValue = {
      val errorsFormat = errors.map( err =>
        Json.obj(
          "error" -> "Form validation error",
            "errorMessage" -> err.message
        )
      )
    Json.toJson(errorsFormat)

  }


}
