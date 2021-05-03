package v1.querier

import org.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Json
import play.api.libs.ws.{WSRequest, WSResponse}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class DruidAPIManagerTest extends PlaySpec
  with MockitoSugar
  with GuiceOneAppPerSuite{


  implicit val executor =  scala.concurrent.ExecutionContext.global

  val druidApi = app.injector.instanceOf[DruidAPIManager]
  val druidApiSpy = spy(druidApi)

  "DruidAPIManager" must {
    "return records in non aggreg DruidRecord object" in {
      val mockWsRequest = mock[WSRequest]
      val mockResponse = mock[WSResponse]

      when(mockResponse.json).thenReturn(
        Json.arr(
          Json.obj(
            "seriesID" -> "12",
            "sensorID" -> "12",
            "__time" -> "2021-05-03T11:14:59Z",
            "address" -> "",
            "city" -> "",
            "country" -> "",
            "description" -> "",
            "measure" -> 4.0,
            "measure_name" -> "temp",
            "name" -> "sensor-temp-granada",
            "region" -> "",
            "sampling_unit" -> "",
            "tags" -> Json.arr("we","aw"),
            "unit" -> "grados"
          ),
          Json.obj(
            "seriesID" -> "12",
            "sensorID" -> "12",
            "__time" -> "2021-05-03T11:15:59Z",
            "address" -> "",
            "city" -> "",
            "country" -> "",
            "description" -> "",
            "measure" -> 1.0,
            "measure_name" -> "temp",
            "name" -> "sensor-temp-granada",
            "region" -> "",
            "sampling_unit" -> "",
            "tags" -> Json.arr("we","aw"),
            "unit" -> "grados"
          ),
          Json.obj(
            "seriesID" -> "12",
            "sensorID" -> "12",
            "__time" -> "2021-05-03T11:16:59Z",
            "address" -> "",
            "city" -> "",
            "country" -> "",
            "description" -> "",
            "measure" -> 43.0,
            "measure_name" -> "temp",
            "name" -> "sensor-temp-granada",
            "region" -> "",
            "sampling_unit" -> "",
            "tags" -> Json.arr("we","aw"),
            "unit" -> "grados"
          )
        )
      )

      when(mockWsRequest.post(Json.obj(
        "query" -> "CONSULTA"
      ))).thenReturn(
        Future{mockResponse}
      )

      when(druidApiSpy.request).thenReturn(mockWsRequest)

      val expectedResults = List(
        DruidRecord("12","12","2021-05-03T11:14:59Z","","","","",4.0,
          "temp","sensor-temp-granada","","",List("we","aw"),"grados"),
        DruidRecord("12","12","2021-05-03T11:15:59Z","","","","",1.0,
          "temp","sensor-temp-granada","","",List("we","aw"),"grados"),
        DruidRecord("12","12","2021-05-03T11:16:59Z","","","","",43.0,
          "temp","sensor-temp-granada","","",List("we","aw"),"grados")
      )

      val responseFuture = druidApiSpy.getRecords("CONSULTA")

      val response = Await.result(responseFuture, Duration.Inf)

      response mustBe expectedResults
      response.size mustBe 3
    }
    "return records with agg measures " in {
      val mockWsRequest = mock[WSRequest]
      val mockResponse = mock[WSResponse]

      when(mockResponse.json).thenReturn(
        Json.arr(
          Json.obj(
            "seriesID" -> "12",
            "sensorID" -> "12",
            "__time" -> "2021-05-03T11:14:59Z",
            "address" -> "",
            "city" -> "",
            "country" -> "",
            "description" -> "",
            "measure" -> 4.0,
            "measure_name" -> "temp",
            "name" -> "sensor-temp-granada",
            "region" -> "",
            "sampling_unit" -> "",
            "tags" -> Json.arr("we","aw"),
            "unit" -> "grados",
            "avg_agg" -> 12.0,
            "min_agg" -> 3.5
          ),
          Json.obj(
            "seriesID" -> "12",
            "sensorID" -> "12",
            "__time" -> "2021-05-03T11:15:59Z",
            "address" -> "",
            "city" -> "",
            "country" -> "",
            "description" -> "",
            "measure" -> 1.0,
            "measure_name" -> "temp",
            "name" -> "sensor-temp-granada",
            "region" -> "",
            "sampling_unit" -> "",
            "tags" -> Json.arr("we","aw"),
            "unit" -> "grados",
            "avg_agg" -> 12.0,
            "min_agg" -> 3.5
          ),
          Json.obj(
            "seriesID" -> "12",
            "sensorID" -> "12",
            "__time" -> "2021-05-03T11:16:59Z",
            "address" -> "",
            "city" -> "",
            "country" -> "",
            "description" -> "",
            "measure" -> 43.0,
            "measure_name" -> "temp",
            "name" -> "sensor-temp-granada",
            "region" -> "",
            "sampling_unit" -> "",
            "tags" -> Json.arr("we","aw"),
            "unit" -> "grados",
            "avg_agg" -> 12.0,
            "min_agg" -> 3.5
          )
        )
      )

      when(mockWsRequest.post(Json.obj(
        "query" -> "CONSULTA"
      ))).thenReturn(
        Future{mockResponse}
      )

      when(druidApiSpy.request).thenReturn(mockWsRequest)

      val expectedResults = List(
        DruidRecord("12","12","2021-05-03T11:14:59Z","","","","",4.0,
          "temp","sensor-temp-granada","","",List("we","aw"),"grados",
          avg_agg = Some(12.0),min_agg = Some(3.5)),
        DruidRecord("12","12","2021-05-03T11:15:59Z","","","","",1.0,
          "temp","sensor-temp-granada","","",List("we","aw"),"grados",
          avg_agg = Some(12.0),min_agg = Some(3.5)),
        DruidRecord("12","12","2021-05-03T11:16:59Z","","","","",43.0,
          "temp","sensor-temp-granada","","",List("we","aw"),"grados",
          avg_agg = Some(12.0), min_agg = Some(3.5))
      )

      val responseFuture = druidApiSpy.getRecords("CONSULTA")

      val response = Await.result(responseFuture, Duration.Inf)

      response mustBe expectedResults
      response.size mustBe 3
    }
  }


}