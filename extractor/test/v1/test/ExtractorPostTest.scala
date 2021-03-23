package v1.test

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.Helpers.{POST, contentAsJson, contentAsString, defaultAwaitTimeout, status}
import play.api.test.{FakeRequest, Helpers}
import v1.extractor.ExtractorController

import scala.concurrent.Future

class ExtractorPostTest extends PlaySpec{
  val controller = new ExtractorController(Helpers.stubControllerComponents())

  "An extractor post request" must {
    "fail if json structure is not valid" in {
      val json = """{"type": "http", "failplease": "failing" }"""

      val request = FakeRequest(POST, "/v1/extractor")
        .withJsonBody(Json.parse(json))
      val result: Future[Result] = controller.post.apply(request)

      val expectedResponse = Json.parse(
      """{"errors":
         [{"dataSchema.sourceID":"error.required"},
          {"dataSchema.sensorIDField":"error.required"},
          {"dataSchema.timestampField":"error.required"},
          {"dataSchema.measures":"No measures"},
          {"IOConfig.address":"error.required"}
          ]}""")

      status(result) mustEqual 400
      contentAsJson(result) mustEqual expectedResponse
    }
    "fail if given type is not valid" in {

      val json = Json.obj(
        "type" -> "notValidType",
        "dataSchema" -> Json.obj(
          "sourceID" -> 1,
          "sensorIDField" -> "id",
          "timestampField" -> "time",
          "measures" -> Json.arr(
            Json.obj(
              "name"-> "temp",
              "field" -> "tempField"
            )
          )
        ),
          "IOConfig" -> Json.obj(
            "address" -> "local",
            "jsonPath" -> "",
            "freq" -> "1"
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

}
