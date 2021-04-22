package v1.test.http

import org.mockito.MockitoSugar
import org.scalatestplus.play._
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.Created
import play.api.test.Helpers.{POST, contentAsJson, contentAsString, defaultAwaitTimeout, status}
import play.api.test.{FakeRequest, Helpers}
import v1.extractor.{ExtractorController, ExtractorForm, ExtractorFormInput, ExtractorServiceImpl}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class HTTPExtractorFormTest extends PlaySpec with MockitoSugar{

  val serviceImplMock = mock[ExtractorServiceImpl]
  val extDataMock = mock[ExtractorFormInput]
  when(serviceImplMock.postExtractor(extDataMock)).thenReturn(Future{Created("Ok")})
  val controller = new ExtractorController(Helpers.stubControllerComponents(), serviceImplMock)

  val addressJsonPath = "https://run.mocky.io/v3/dec8605f-60b5-4517-bcba-427ac5e316f4"
  val addressSingleSource = "https://run.mocky.io/v3/f6abb769-7fc6-4e19-9313-e09096de138c"
  val addressList =  "https://run.mocky.io/v3/c4017730-abcc-43ac-b28e-5292ddddc9e8"
  val emptyMeasure = "https://run.mocky.io/v3/08884f55-3e2c-4dd1-a79d-9d2251a7505a"
  val wrongTypeAdress = "https://run.mocky.io/v3/524d0930-1bc4-4d53-94ac-87894624bf42"
  val wrongContentType = "https://run.mocky.io/v3/9f143a4f-b194-40a3-9fae-0f6bd215c018"

  "A post request of an http source" must {
    "fail when url is not responding" in {
      val json = Json.obj(
        "type" -> "http",
        "dataSchema" -> Json.obj(

          "sensorIDField" -> "id",
          "timestampField" -> "time",
          "measures" -> Json.arr(
            Json.obj(
              "name"-> "temp",
              "field" -> "tempField",
              "unit" -> "grades"
            )
          )
        ),
        "IOConfig" -> Json.obj(
          "inputConfig" -> Json.obj(
            "address" -> "local",
            "jsonPath" -> "$",
            "freq" -> 1
          ),
          "kafkaConfig" -> Json.obj(
            "topic" -> "test",
            "server" -> "localhost:9092"
          )),
        "metadata" -> Json.obj(
          "name" -> "santander-traffic",
          "sample" -> Json.obj(
            "freq" -> "1",
            "unit" -> "seconds"
          ),
          "localization" -> Json.obj(
            "name" -> "Santander"
          )
        )
      )
      val request = FakeRequest(POST, "/v1/extractor")
        .withJsonBody(json)

      val result: Future[Result] = controller.post.apply(request)
      val response = contentAsJson(result)

      val expectedResponse = Json.parse(
       """{"errors":[{"general":"Cannot determine target address"}]}"""
      )

      status(result) mustEqual 400
      response mustEqual expectedResponse

    }
    "fail if no jsonPath or freq provided" in {
      val json = Json.obj(
        "type" -> "http",
        "dataSchema" -> Json.obj(
          "sensorIDField" -> "id",
          "timestampField" -> "time",
          "measures" -> Json.arr(
            Json.obj(
              "name"-> "temp",
              "field" -> "tempField",
              "unit" -> "grades"
            )
          )
        ),
        "IOConfig" -> Json.obj(
          "inputConfig" -> Json.obj(
            "address" -> addressList,
          ),
          "kafkaConfig" -> Json.obj(
            "topic" -> "test",
            "server" -> "localhost:9092"
          )),
      "metadata" -> Json.obj(
        "name" -> "santander-traffic",
        "sample" -> Json.obj(
          "freq" -> "1",
          "unit" -> "seconds"
        ),
        "localization" -> Json.obj(
          "name" -> "Santander"
        )
      )
      )
      val request = FakeRequest(POST, "/v1/extractor")
        .withJsonBody(json)

      val result: Future[Result] = controller.post.apply(request)
      val response = contentAsJson(result)

      val expectedResponse = Json.parse(
        """
          |{"errors":[
          |{"general":"You must set freq variable for HttpExtractor"},
          |{"general":"You must set jsonPath variable for HttpExtractor"}
          |]}""".stripMargin)

      status(result) mustEqual 400
      response mustEqual expectedResponse

    }

    "fail if provided schema cant match original one" in {
      val json = Json.obj(
        "type" -> "http",
        "dataSchema" -> Json.obj(
          "sensorIDField" -> "id",
          "timestampField" -> "time",
          "measures" -> Json.arr(
            Json.obj(
              "name"-> "temp",
              "field" -> "tempField",
              "unit" -> "grades"
            )
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
          )),
        "metadata" -> Json.obj(
          "name" -> "santander-traffic",
          "sample" -> Json.obj(
            "freq" -> "1",
            "unit" -> "seconds"
          ),
          "localization" -> Json.obj(
            "name" -> "Santander"
          )
        )
      )
      val request = FakeRequest(POST, "/v1/extractor")
        .withJsonBody(json)

      val result: Future[Result] = controller.post.apply(request)
      val response = contentAsJson(result)
      val expectedResponse = Json.parse(
        """
          {"errors":
            [{"general":"Can't find timestamp field in JSON"},
            {"general":"Can't find sensorID field in JSON"},
            {"general":"Measure temp not found in source"}]
          }
          """
      )
      status(result) mustBe 400
      response mustBe expectedResponse


    }
    "fail if provided source data types are wrong" in {

      val json = Json.obj(
        "type" -> "http",
        "dataSchema" -> Json.obj(
          "sensorIDField" -> "ayto:idSensor",
          "timestampField" -> "dc:modified",
          "measures" -> Json.arr(
            Json.obj(
              "name"-> "ocupation",
              "field" -> "ayto:ocupacion",
              "unit" -> "grades"
            ),
            Json.obj(
              "name"-> "intensity",
              "field" -> "ayto:intensidad",
              "unit" -> "grades"
            ),
          )
        ),
        "IOConfig" -> Json.obj(
          "inputConfig" -> Json.obj(
            "address" -> wrongTypeAdress,
            "jsonPath" -> "$",
            "freq" -> 1
          ),
          "kafkaConfig" -> Json.obj(
            "topic" -> "test",
            "server" -> "localhost:9092"
          ))
        ,
        "metadata" -> Json.obj(
          "name" -> "santander-traffic",
          "sample" -> Json.obj(
            "freq" -> "1",
            "unit" -> "seconds",

          ),
          "localization" -> Json.obj(
            "name" -> "Santander"
          )
        )
      )
      val request = FakeRequest(POST, "/v1/extractor")
        .withJsonBody(json)

      val result: Future[Result] = controller.post.apply(request)
      val response = contentAsJson(result)
      val expectedResponse = Json.parse(
        """
          {"errors":
            [{"general":"Timestamp is not a valid ISO date"},
            {"general":"Number expected but other type found in ayto:intensidad"}
            ]
          }
          """
      )
      status(result) mustBe 400
      response mustBe expectedResponse
    }
    "fail if source content type is not JSON" in{

      val json = Json.obj(
        "type" -> "http",
        "dataSchema" -> Json.obj(
          "sensorIDField" -> "ayto:idSensor",
          "timestampField" -> "dc:modified",
          "measures" -> Json.arr(
            Json.obj(
              "name"-> "ocupation",
              "field" -> "ayto:ocupacion",
              "unit" -> "grades"
            ),
            Json.obj(
              "name"-> "intensity",
              "field" -> "ayto:intensidad",
              "unit" -> "grades"
            ),
          )
        ),
        "IOConfig" -> Json.obj(
          "inputConfig" -> Json.obj(
            "address" -> wrongContentType,
            "jsonPath" -> "$",
            "freq" -> 1
          ),
          "kafkaConfig" -> Json.obj(
            "topic" -> "test",
            "server" -> "localhost:9092"
          ))
        ,
        "metadata" -> Json.obj(
          "name" -> "santander-traffic",
          "sample" -> Json.obj(
            "freq" -> "1",
            "unit" -> "seconds"
          ),
          "localization" -> Json.obj(
            "name" -> "Santander"
          )
        )
      )
      val request = FakeRequest(POST, "/v1/extractor")
        .withJsonBody(json)

      val result: Future[Result] = controller.post.apply(request)
      val response = contentAsJson(result)
      val expectedResponse = Json.parse(
        """
          {"errors":[{"general":"Source extraction error while parsing JSON (check content type and jsonPath)"}]}
          """
      )

      status(result) mustBe 400
      response mustBe expectedResponse


    }
    "fail if jsonPath is wrong" in {

      val json = Json.obj(
        "type" -> "http",
        "dataSchema" -> Json.obj(
          "sensorIDField" -> "ayto:idSensor",
          "timestampField" -> "dc:modified",
          "measures" -> Json.arr(
            Json.obj(
              "name"-> "ocupation",
              "field" -> "ayto:ocupacion",
              "unit" -> "grades"
            ),
            Json.obj(
              "name"-> "intensity",
              "field" -> "ayto:intensidad",
              "unit" -> "grades"
            ),
          )
        ),
        "IOConfig" -> Json.obj(
          "inputConfig" -> Json.obj(
            "address" -> addressJsonPath,
            "jsonPath" -> "$",
            "freq" -> 1
          ),
          "kafkaConfig" -> Json.obj(
            "topic" -> "test",
            "server" -> "localhost:9092"
          )),
        "metadata" -> Json.obj(
          "name" -> "santander-traffic",
          "sample" -> Json.obj(
            "freq" -> "1",
            "unit" -> "seconds"
          ),
          "localization" -> Json.obj(
            "name" -> "Santander"
          )
        )
      )
      val request = FakeRequest(POST, "/v1/extractor")
        .withJsonBody(json)

      val result: Future[Result] = controller.post.apply(request)
      val response = contentAsJson(result)

      println(contentAsString(result))

      status(result) mustBe 400
    }

    "work if everything is correct" in {
      val correctAdresses = Seq(
        Tuple2(addressJsonPath, "$.resources"),
        Tuple2(addressSingleSource,"$"),
        Tuple2(addressList,"$"),
        Tuple2(emptyMeasure,"$"))
      correctAdresses.foreach( tuple => {
        val json = Json.obj(
          "type" -> "http",
          "dataSchema" -> Json.obj(
            "sensorIDField" -> "ayto:idSensor",
            "timestampField" -> "dc:modified",
            "measures" -> Json.arr(
              Json.obj(
                "name"-> "ocupation",
                "field" -> "ayto:ocupacion",
                "unit" -> "veh/h"
              ),
              Json.obj(
                "name"-> "intensity",
                "field" -> "ayto:intensidad",
                "unit" -> "%"
              ),
            )
          ),
          "IOConfig" -> Json.obj(
            "inputConfig" -> Json.obj(
              "address" -> tuple._1,
              "jsonPath" -> tuple._2,
              "freq" -> 1
            ),
            "kafkaConfig" -> Json.obj(
              "topic" -> "test",
              "server" -> "localhost:9092"
            )),
          "metadata" -> Json.obj(
            "name" -> "santander-traffic",
            "sample" -> Json.obj(
              "freq" -> "1",
              "unit" -> "seconds"
            ),
            "localization" -> Json.obj(
              "name" -> "Santander"
            )
          )
        )
        val correctValidation = ExtractorForm.form.bind(json,100000).fold(
          formWithErrors => {
            false
          },
          extData => {
          true
        }
        )
        correctValidation mustBe true
      })


    }

  }
}
