package v1.test

import org.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Results.{Created, Ok}
import play.api.mvc._
import play.api.test.Helpers.{DELETE, GET, POST, PUT, contentAsJson, contentAsString, defaultAwaitTimeout, status}
import play.api.test.{FakeRequest, Helpers}
import v1.extractor.models.extractor.config.KafkaConfig
import v1.extractor.models.extractor.{DataSchema, MeasureField}
import v1.extractor.models.metadata.{Location, Metadata, Sample}
import v1.extractor.{ExtractorController, ExtractorFormInput, ExtractorServiceImpl, ExtractorType, IOConfigForm, InputConfigForm}

import java.time.LocalDateTime
import java.util.Date
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ExtractorControllerTest extends PlaySpec
  with MockitoSugar
{
  val serviceImplMock: ExtractorServiceImpl = mock[ExtractorServiceImpl]
  val id = 1

  val metadata = Metadata("traffic-santander",Some("Santander traffic flow sensors"),Seq("traffic","static"),
    new Sample(1,"seconds"),
    new Location("santander city",city=Some("Santander"),region=Some("Santander"),country=Some("Spain")),
    url = Some("http://datos.santander.es/dataset/?id=datos-trafico"))

  val extData = ExtractorFormInput("http",
    DataSchema("ayto:idSensor", "dc:modified",  List(
      new MeasureField("ocupation","ayto:ocupacion","veh/h"
        ,Some("Vehículos por hora sobre una espiga")),
      new MeasureField("intensity", "ayto:intensidad","%"))),
    IOConfigForm(
      InputConfigForm("https://run.mocky.io/v3/c4017730-abcc-43ac-b28e-5292ddddc9e8",Some("$")
         ,Some(1)),KafkaConfig("test","localhost:9092")),metadata)

  val fakeID = "1"
  when(serviceImplMock.postExtractor(extData)).thenReturn(Future{Created("Ok")})
  when(serviceImplMock.getExtractor(fakeID)).thenReturn(Future{Ok("ok")})
  when(serviceImplMock.deleteExtractor(fakeID)).thenReturn(Future{Ok("ok")})
  when(serviceImplMock.startExtractor(fakeID)).thenReturn(Future{Ok("ok")})
  when(serviceImplMock.stopExtractor(fakeID)).thenReturn(Future{Ok("ok")})

  val controller = new ExtractorController(Helpers.stubControllerComponents(), serviceImplMock)
  val addressList =  "https://run.mocky.io/v3/c4017730-abcc-43ac-b28e-5292ddddc9e8"

  val validJson: JsObject =Json.obj(
    "type" -> "http",
    "dataSchema" -> Json.obj(
      "sensorIDField" -> "ayto:idSensor",
      "timestampField" -> "dc:modified",
      "measures" -> Json.arr(
        Json.obj(
          "name"-> "ocupation",
          "field" -> "ayto:ocupacion",
          "unit" -> "veh/h",
          "description" -> "wewe"
        ),
        Json.obj(
          "name"-> "intensity",
          "field" -> "ayto:intensidad",
          "unit" -> "%",
          "description" -> "wewe"
        )
      )
    ),
    "IOConfig" -> Json.obj(
      "inputConfig" -> Json.obj(
        "address" -> addressList,
        "jsonPath" -> "$",
        "freq" -> "1"
      ),
      "kafkaConfig" -> Json.obj(
        "topic" -> "test",
        "server" -> "localhost:9092"
      )),
    "metadata" -> Json.obj(
      "name" -> "santander-traffic",
      "description" -> "Santander traffic sensors blabla",
      "sample" -> Json.obj(
        "freq" -> "1",
        "unit" -> "seconds"
      ),
      "localization" -> Json.obj(
        "name" -> "Santander",
        "city" -> "Santander",
        "region" -> "Santander",
        "country" -> "Spain",
        "address" -> "Centro de logística de datos de Santander"
      ),
      "url" -> "wewe.es",
    )
  )

  "An extractor post request" must {
    "fail if json structure is not valid" in {
      val json = """{"tye": "http", "failplease": "failing" }"""

      val request = FakeRequest(POST, "/v1/extractor")
        .withJsonBody(Json.parse(json))
      val result: Future[Result] = controller.post.apply(request)

      val expectedResponse = Json.parse(
      """{"errors":[{"type":"error.required"},{"dataSchema.sensorIDField":"error.required"},{"dataSchema.timestampField":"error.required"},{"dataSchema.measures":"No measures"},{"IOConfig.inputConfig.address":"error.required"},{"IOConfig.kafkaConfig.topic":"error.required"},{"IOConfig.kafkaConfig.server":"error.required"},{"metadata.name":"error.required"},{"metadata.sample.freq":"error.required"},{"metadata.sample.unit":"error.required"},{"metadata.localization.name":"error.required"}]}""")

      status(result) mustEqual 400
      contentAsJson(result) mustEqual expectedResponse
    }
    "fail if given type is not valid" in {
      val json = Json.obj(
        "id" -> 1,
        "type" -> "notValidType",
        "dataSchema" -> Json.obj(
          "sensorIDField" -> "id",
          "timestampField" -> "time",
          "measures" -> Json.arr(
            Json.obj(
              "name"-> "temp",
              "field" -> "tempField",
              "measureID" -> 1,
              "unit" -> "grados"
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
            )),
      "metadata" -> Json.obj(
        "name" -> "santander-traffic",
        "description" -> "Santander traffic sensors blabla",
        "sample" -> Json.obj(
          "freq" -> "1",
          "unit" -> "seconds"
        ),
        "localization" -> Json.obj(
          "name" -> "Santander",
          "city" -> "Santander",
          "region" -> "Santander",
          "country" -> "Spain",
          "address" -> "Centro de logística de datos de Santander"
        ),
      "url" -> "wewe.es",
      "installationDate" -> "2021-04-22T11:49:20Z"

      )
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
