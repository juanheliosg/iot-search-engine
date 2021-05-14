package v1.querier

import org.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Result
import play.api.test.{FakeRequest, Helpers}
import play.api.test.Helpers.{POST, contentAsJson, contentAsString, defaultAwaitTimeout, status}
import v1.querier.models.Query

import scala.concurrent.Future

class ControllerProcessorTest extends PlaySpec
  with MockitoSugar{

  implicit val executor =  scala.concurrent.ExecutionContext.global

  val mockDruidApi = mock[DruidAPIManager]
  val mockTsApi = mock[TsAnalysisManager]
  val controller: QuerierController = new QuerierController(Helpers.stubControllerComponents(),mockDruidApi, mockTsApi)
  val controllerSpy: QuerierController = spy(controller)
  val query: Query = Query(100,List(("2021-05-04T19:00:29Z","2021-05-04T19:00:29Z")),true,"simple","tags='smartcity'")
  val jsonQuery: JsObject = Json.obj(
    "limit" -> 100,
    "timeRange" -> Json.arr(
      Json.obj(
        "lowerBound" ->"2021-05-04T19:00:29Z",
        "upperBound" -> "2021-05-04T19:00:29Z"
      )
    ),
    "type" -> "simple",
    "filter" -> "tags='smartcity'")

  val records: List[DruidRecord] = SampleRecords.records

  when(controllerSpy.getRawRecords(query)).thenReturn(
    Future{Left(records)}
  )


  "querier controller and processor" must {
    "return list of query response if everything ok" in {
      val request = FakeRequest(POST, "/v1/extractor")
        .withJsonBody(jsonQuery)
      val result: Future[Result] = controller.postQuery.apply(request)

      status(result) mustEqual 200
      (contentAsJson(result) \ "items").as[Int] mustBe 2

      true mustBe true

    }
  }

}
