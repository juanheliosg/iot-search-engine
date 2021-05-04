package v1.querier

import play.api.data.FormError
import play.api.libs.json.{JsArray, JsValue, Json}

object JSONError{
  def format(errors: Seq[FormError]): JsValue = {
    Json.arr(
      errors.map( err =>
        Json.obj(
          "error" -> err.key,
            "errorMessage" -> err.message
        )
      )
    )
  }

  def format(error: DruidError): JsValue = {
    Json.arr(
        Json.obj(
          "error" -> error.error,
          "errorMessage" -> error.errorMessage
      )
    )
  }

}
