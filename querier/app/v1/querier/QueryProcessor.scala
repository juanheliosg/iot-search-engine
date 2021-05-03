package v1.querier

import play.api.libs.json.{JsObject, Json}

object QueryProcessor{

  /**
   * Returns a query response object from an SQL query
   * @param query
   */
  def resolveQuery(query: String): JsObject = {
    Json.obj()
  }

}
