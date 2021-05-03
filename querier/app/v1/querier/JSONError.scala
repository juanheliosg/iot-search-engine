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

}
