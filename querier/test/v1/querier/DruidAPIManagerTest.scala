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
      when(mockResponse.status).thenReturn(200)
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
            "sampling_freq" -> "",
            "measure_desc" -> "",
            "coordinates" -> Json.arr("",""),
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
            "sampling_freq" -> "",
            "measure_desc" -> "",
            "coordinates" -> Json.arr("",""),
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
            "sampling_freq" -> "",
            "measure_desc" -> "",
            "coordinates" -> Json.arr("",""),
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
        DruidRecord(seriesID = "12",
          sensorID = "12",
          __time = "2021-05-03T11:14:59Z",
          address = "",
          city = "",
          country = "",
          description = "",
          measure = 4.0,
          measure_name = "temp",
          unit ="grados",
          measure_desc = "",
          name = "sensor-temp-granada",
          region = "",
          sampling_unit = "",
          sampling_freq = "",
          coordinates = List("",""),
          tags = List("we","aw")),
        DruidRecord(seriesID = "12",
          sensorID = "12",
          __time = "2021-05-03T11:15:59Z",
          address = "",
          city = "",
          country = "",
          description = "",
          measure = 1.0,
          measure_name = "temp",
          unit ="grados",
          measure_desc = "",
          name = "sensor-temp-granada",
          region = "",
          sampling_unit = "",
          sampling_freq = "",
          coordinates = List("",""),
          tags = List("we","aw")),
        DruidRecord(seriesID = "12",
          sensorID = "12",
          __time = "2021-05-03T11:16:59Z",
          address = "",
          city = "",
          country = "",
          description = "",
          measure = 43,
          measure_name = "temp",
          unit ="grados",
          measure_desc = "",
          name = "sensor-temp-granada",
          region = "",
          sampling_unit = "",
          sampling_freq = "",
          coordinates = List("",""),
          tags = List("we","aw"))
      )

      val responseFuture = druidApiSpy.getRecords("CONSULTA")

      val response = Await.result(responseFuture, Duration.Inf)

      response match{
        case Left(value) =>
          value mustBe expectedResults
          value.size mustBe 3
        case Right(value) =>{

          fail()
        }
      }
    }
    "return records with agg measures " in {
      val mockWsRequest = mock[WSRequest]
      val mockResponse = mock[WSResponse]

      when(mockResponse.status).thenReturn(200)
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
            "sampling_freq" -> "",
            "name" -> "sensor-temp-granada",
            "region" -> "",
            "sampling_unit" -> "",
            "tags" -> Json.arr("we","aw"),
            "unit" -> "grados",
            "measure_desc" -> "",
            "coordinates" -> Json.arr("",""),
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
            "sampling_freq" -> "",
            "measure_desc" -> "",
            "coordinates" -> Json.arr("",""),
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
            "measure_desc" -> "",
            "coordinates" -> Json.arr("",""),
            "measure" -> 43.0,
            "measure_name" -> "temp",
            "name" -> "sensor-temp-granada",
            "region" -> "",
            "sampling_unit" -> "",
            "sampling_freq" -> "",
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
        DruidRecord(seriesID = "12",
          sensorID = "12",
          __time = "2021-05-03T11:14:59Z",
          address = "",
          city = "",
          country = "",
          description = "",
          measure = 4.0,
          measure_name = "temp",
          unit ="grados",
          measure_desc = "",
          name = "sensor-temp-granada",
          region = "",
          sampling_unit = "",
          sampling_freq = "",
          coordinates = List("",""),
          tags = List("we","aw"),
          avg_agg = Some(12.0),
          min_agg = Some(3.5)),
        DruidRecord(seriesID = "12",
          sensorID = "12",
          __time = "2021-05-03T11:15:59Z",
          address = "",
          city = "",
          country = "",
          description = "",
          measure = 1.0,
          measure_name = "temp",
          unit ="grados",
          measure_desc = "",
          name = "sensor-temp-granada",
          region = "",
          sampling_unit = "",
          sampling_freq = "",
          coordinates = List("",""),
          tags = List("we","aw"),
          avg_agg = Some(12.0),
          min_agg = Some(3.5)),
        DruidRecord(seriesID = "12",
          sensorID = "12",
          __time = "2021-05-03T11:16:59Z",
          address = "",
          city = "",
          country = "",
          description = "",
          measure = 43,
          measure_name = "temp",
          unit ="grados",
          measure_desc = "",
          name = "sensor-temp-granada",
          region = "",
          sampling_unit = "",
          sampling_freq = "",
          coordinates = List("",""),
          tags = List("we","aw"),
          avg_agg = Some(12.0),
          min_agg = Some(3.5))
      )

      val responseFuture = druidApiSpy.getRecords("CONSULTA")

      val response = Await.result(responseFuture, Duration.Inf)
      response match{
        case Left(value) =>
          value mustBe expectedResults
          value.size mustBe 3
        case Right(value) =>{
          fail()
        }
      }
    }
    "return error if wrong json" in {
      val mockWsRequest = mock[WSRequest]
      val mockResponse = mock[WSResponse]

      when(mockResponse.status).thenReturn(200)
      when(mockResponse.json).thenReturn(
        Json.arr(
          Json.obj(
            "seriesID" -> "12",
            "sensorID" -> "12",
            "__time" -> "2021-05-03T11:14:59Z",
            "address" -> "")
        ))

      when(mockWsRequest.post(Json.obj(
        "query" -> "CONSULTA"
      ))).thenReturn(
        Future{mockResponse}
      )

      when(druidApiSpy.request).thenReturn(mockWsRequest)

      val responseFuture = druidApiSpy.getRecords("CONSULTA")

      val response = Await.result(responseFuture, Duration.Inf)
      response match{
        case Left(_) =>
          fail()
        case Right(value) =>{
          value(0).error mustBe "Json parsing error"
        }
      }

    }
    "return druid error if druid error is sent" in {
      val mockWsRequest = mock[WSRequest]
      val mockResponse = mock[WSResponse]

      when(mockResponse.status).thenReturn(400)
      when(mockResponse.json).thenReturn(
        Json.obj(
          "error" -> "SQL parsing error",
          "errorMessage" -> "Fail parsing where clausule some other things expected",
          "errorClass" -> "FBD.suspended.exception",
          "host" -> "yourlocaldruid.com"
        )
      )

      when(mockWsRequest.post(Json.obj(
        "query" -> "CONSULTA"
      ))).thenReturn(
        Future{mockResponse}
      )

      when(druidApiSpy.request).thenReturn(mockWsRequest)

      val responseFuture = druidApiSpy.getRecords("CONSULTA")

      val response = Await.result(responseFuture, Duration.Inf)
      response match{
        case Left(_) =>
          fail()
        case Right(value) =>{
          value(0).error mustBe "SQL parsing error"
        }
      }
    }
  }


}