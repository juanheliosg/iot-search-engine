package v1.extractor

import play.api.libs.json.{JsObject, JsValue, Json}

object JSONError {
  def format(errors: JsValue): JsObject = {
    Json.obj("errors" -> Json.arr(
      errors
    )
    )
  }
}
