package v1.test

import org.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Results.{Created, Ok}
import play.api.mvc._
import play.api.test.Helpers.{DELETE, GET, POST, PUT, contentAsJson, defaultAwaitTimeout, status}
import play.api.test.{FakeRequest, Helpers}
import v1.extractor.{DataSchema, ExtractorController, ExtractorFormInput, ExtractorServiceImpl, ExtractorType, IOConfigForm, InputConfigForm, KafkaConfig, Measure}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ExtractorControllerTest extends PlaySpec
  with MockitoSugar
{
  val serviceImplMock: ExtractorServiceImpl = mock[ExtractorServiceImpl]
  val id = 1
  val extData = ExtractorFormInput(1, "http",
    DataSchema(1,"ayto:idSensor","dc:modified",
      List(Measure("ocupation","ayto:ocupacion",2),
        Measure("intensity","ayto:intensidad",1))),
    IOConfigForm(
      InputConfigForm("https://run.mocky.io/v3/c4017730-abcc-43ac-b28e-5292ddddc9e8",Some("$")
         ,Some(1)),KafkaConfig("test","localhost:9092")))

  when(serviceImplMock.postExtractor(extData)).thenReturn(Future{Created("Ok")})
  when(serviceImplMock.updateExtractor(id, extData)).thenReturn(Future{Ok("ok")})
  when(serviceImplMock.getExtractor(id)).thenReturn(Future{Ok("ok")})
  when(serviceImplMock.deleteExtractor(id)).thenReturn (Future{Ok("ok")})
  when(serviceImplMock.startExtractor(id)).thenReturn(Future{Ok("ok")})
  when(serviceImplMock.stopExtractor(id)).thenReturn(Future{Ok("ok")})

  val controller = new ExtractorController(Helpers.stubControllerComponents(), serviceImplMock)
  val addressList =  "https://run.mocky.io/v3/c4017730-abcc-43ac-b28e-5292ddddc9e8"

  val validJson: JsObject = Json.obj(
    "id" -> id,
    "type" -> "http",
    "dataSchema" -> Json.obj(
      "sourceID" -> 1,
      "sensorIDField" -> "ayto:idSensor",
      "timestampField" -> "dc:modified",
      "measures" -> Json.arr(
        Json.obj(
          "name"-> "ocupation",
          "field" -> "ayto:ocupacion",
          "measureID" -> 2
        ),
        Json.obj(
          "name"-> "intensity",
          "field" -> "ayto:intensidad",
          "measureID" -> 1
        ),
      )
    ),
    "IOConfig" -> Json.obj(
      "inputConfig" -> Json.obj(
        "address" -> addressList,
        "jsonPath" -> "$",
        "freq" -> 1
      ),
      "kafkaConfig" -> Json.obj(
        "topic" -> "test",
        "server" -> "localhost:9092"
      )))

  "An extractor post request" must {
    "fail if json structure is not valid" in {
      val json = """{"tye": "http", "failplease": "failing" }"""

      val request = FakeRequest(POST, "/v1/extractor")
        .withJsonBody(Json.parse(json))
      val result: Future[Result] = controller.post.apply(request)

      val expectedResponse = Json.parse(
      """{"errors":
         [
         {"id": "error.required"},
         {"type":"error.required"},
         {"dataSchema.sourceID":"error.required"},
          {"dataSchema.sensorIDField":"error.required"},
          {"dataSchema.timestampField":"error.required"},
          {"dataSchema.measures":"No measures"},
          {"IOConfig.inputConfig.address":"error.required"},
          {"IOConfig.kafkaConfig.topic":"error.required"},
          {"IOConfig.kafkaConfig.server":"error.required"}
          ]}""")

      status(result) mustEqual 400
      contentAsJson(result) mustEqual expectedResponse
    }
    "fail if given type is not valid" in {

      val json = Json.obj(
        "id" -> 1,
        "type" -> "notValidType",
        "dataSchema" -> Json.obj(
          "sourceID" -> 1,
          "sensorIDField" -> "id",
          "timestampField" -> "time",
          "measures" -> Json.arr(
            Json.obj(
              "name"-> "temp",
              "field" -> "tempField",
              "measureID" -> 1
            )
          )
        ),
          "IOConfig" -> Json.obj(
            "inputConfig" -> Json.obj(
            "address" -> "local",
            "jsonPath" -> "",
            "freq" -> "1"
            ),
            "kafkaConfig" -> Json.obj(
              "topic" -> "test",
              "server" -> "localhost:9092"
            ))
          )

      val request = FakeRequest(POST, "/v1/extractor")
        .withJsonBody(json)
      val result: Future[Result] = controller.post.apply(request)


      val response = contentAsJson(result)
      val expectedResponse = Json.parse("""{"errors":[{"type":"Bad type"}]}""")

      status(result) mustEqual 400
      response mustEqual expectedResponse
    }
  }
  "a GET /v1/extractor/id request" must {
    "work if everything correct" in{
      val request = FakeRequest(GET, s"/v1/extractor/$id")
      val result: Future[Result] = controller.get(id).apply(request)
      status(result) mustBe 200

    }
  }
  "a PUT /v1/extractor/id request" must {
    "fail if url id does not match body id" in {
      val newId = id+1
      val request = FakeRequest(PUT, s"/v1/extractor/$newId")
        .withJsonBody(validJson)
      val result: Future[Result] = controller.put(newId).apply(request)
      status(result) mustBe 400
    }
    "work if everything correct" in{
      val request = FakeRequest(PUT, s"/v1/extractor/$id")
        .withJsonBody(validJson)
      val result: Future[Result] = controller.put(id).apply(request)
      status(result) mustBe 200
    }
  }
  "a DELETE /v1/extractor/id request" must{
    "work if everything correct" in{
      val request = FakeRequest(DELETE, s"/v1/extractor/$id")
      val result: Future[Result] = controller.delete(id).apply(request)
      status(result) mustBe 200

    }
  }
  "a GET /v1/extractor/id/start request" must {
    "work if everything correct" in{
      val request = FakeRequest(GET, s"/v1/extractor/$id/start")
      val result: Future[Result] = controller.getStart(id).apply(request)
      status(result) mustBe 200

    }
  }
  "a GET /v1/extractor/id/stop request" must {
    "work if everything correct" in{
      val request = FakeRequest(GET, s"/v1/extractor/$id/stop")
      val result: Future[Result] = controller.getStop(id).apply(request)
      status(result) mustBe 200
    }
  }


}
