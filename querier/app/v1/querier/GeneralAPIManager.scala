package v1.querier

import play.api.libs.json.{JsError, JsObject, JsSuccess, Reads}
import play.api.libs.ws.{WSRequest, WSResponse}

import scala.concurrent.{ExecutionContext, Future}


trait GeneralAPIManager {

  protected def postGeneralQuery[Result,Error](data: JsObject, request: WSRequest, errorMapper: Error => List[JSONError], resultPath: String = "")
                                    (implicit rds: Reads[Result], ec: ExecutionContext, rdserror: Reads[Error]):
  Future[Either[List[Result],List[JSONError]]] = {
    try {
      val response: Future[WSResponse] = request.post(data)
      response.map(resp => {
        if (resp.status == 200) {
          val respJson = resultPath match{
            case "" =>
              resp.json
            case _ =>
              (resp.json \ resultPath)
          }
          respJson.validate[List[Result]] match {
            case JsSuccess(value, path) => Left(value)
            case JsError(errors) =>
              Right(errors.map(er => JSONError("Json parsing error",s"${er._2(0).message} ${er._1.toString()}" )).toList)
          }
        }
        else {
          Right(resp.json.validate[Error] match {
            case JsSuccess(value, path) => errorMapper(value)
            case JsError(errors) =>
              List(JSONError("Json parsing error after error result", errors.flatMap(er => er._2.map(_.message).mkString(" ")
              ).mkString("")))
          }
          )
        }
      }
      )
    } catch {
      case _ =>
        Future{Right(
        List(JSONError("General error", "Probably an HTML response instead of json"))
        )}
    }
  }

}