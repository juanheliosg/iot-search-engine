package v1.querier

import play.api.libs.json.{JsError, JsObject, JsSuccess, Reads}
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}

import scala.concurrent.{ExecutionContext, Future}


trait GeneralAPIManager {

  protected def postGeneralQuery[Result,Error](data: JsObject, request: WSRequest, errorMapper: Error => List[JSONError])
                                    (implicit rds: Reads[Result], ec: ExecutionContext, rdserror: Reads[Error]):
  Future[Either[List[Result],List[JSONError]]] = {

    val response: Future[WSResponse] = request.post(data)
    response.map(resp => {
      if(resp.status == 200){
        resp.json.validate[List[Result]] match{
          case JsSuccess(value, path) => Left(value)
          case JsError(errors) =>
            Right(List(JSONError("Json parsing error",errors.flatMap(
              er => er._2.flatMap(valError => valError.message).mkString("\n")).mkString("\n"))
            ))
        }
      }
      else{
        Right(resp.json.validate[Error] match{
          case JsSuccess(value, path) => errorMapper(value)
          case JsError(errors) =>
            List(JSONError("Json parsing error",errors.flatMap(
              er => er._2.flatMap(valError => valError.message).mkString("\n")).mkString("\n")))
        }
        )}
    }
    )
  }


}