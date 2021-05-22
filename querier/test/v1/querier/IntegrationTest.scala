package v1.querier


import akka.util.Timeout
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, POST, contentAsJson, contentAsString, status}
import v1.querier.models.Subsequence

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{OffsetDateTime, ZoneOffset}
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class IntegrationTest extends PlaySpec with GuiceOneAppPerSuite{
  /**
   * A druid and tsanalysis instance must be up on localhost directions specified in conf.
   */
  implicit val timeout: Timeout = Timeout(60.seconds)
  val controller = app.injector.instanceOf[QuerierController]
  val limit = 100
  "getting field count responses must work" in{

    val request = FakeRequest(GET, "/v1/measures")
    val result: Future[Result] = controller.getMeasuresName.apply(request)
    println(contentAsString(result))
    status(result) mustEqual 200

  }
  "getting correct answer to basic queries without ts" in {
    val jsonQuery: JsObject = Json.obj(
      "limit" -> limit,
      "timeRange" -> Json.arr(
        Json.obj(
          "lowerBound" -> OffsetDateTime.now( ZoneOffset.UTC ).minus(1, ChronoUnit.HOURS).format(DateTimeFormatter.ISO_DATE_TIME) ,
          "upperBound" ->OffsetDateTime.now( ZoneOffset.UTC ).format(DateTimeFormatter.ISO_DATE_TIME)
        )
      ),
      "type" -> "simple",
      "filter" -> "tags LIKE 'smartcity' ")

    val request = FakeRequest(POST, "/v1/query").withJsonBody(jsonQuery)
    val result: Future[Result] = controller.postQuery.apply(request)

    status(result) mustEqual 200
    println(contentAsString(result))
    assert((contentAsJson(result) \ "items").as[Int] <= limit)
    ((contentAsJson(result) \ "series")(0) \ "timestamps").as[Seq[String]].isEmpty mustBe true
    ((contentAsJson(result) \ "series")(0) \ "values").as[Seq[BigDecimal]].isEmpty mustBe true
  }
  "getting correct answer to basic queries with time series" in {
    val jsonQuery: JsObject = Json.obj(
      "limit" -> limit,
      "timeRange" -> Json.arr(
        Json.obj(
          "lowerBound" -> OffsetDateTime.now( ZoneOffset.UTC ).minus(1, ChronoUnit.HOURS).format(DateTimeFormatter.ISO_DATE_TIME) ,
          "upperBound" ->OffsetDateTime.now( ZoneOffset.UTC ).format(DateTimeFormatter.ISO_DATE_TIME)
        )
      ),
      "timeseries" -> true,
      "type" -> "simple",
      "filter" -> "tags LIKE 'smartcity' ")

    val request = FakeRequest(POST, "/v1/query").withJsonBody(jsonQuery)
    val result: Future[Result] = controller.postQuery.apply(request)
    val jsonResult = contentAsJson(result)
    println(contentAsString(result))
    status(result) mustEqual 200
    assert((jsonResult \ "items").as[Int] <= limit)
    ((jsonResult \ "series")(0) \ "timestamps").as[Seq[String]].isEmpty mustBe false
    ((jsonResult \ "series")(0) \ "values").as[Seq[BigDecimal]].isEmpty mustBe false
  }
  "getting correct answers to aggregate queries without ts" in {
    //Getting sensors with ocupation measures greater whose average in the last hour
    //is greater than 50, also get the max measure in that hour
    val jsonQuery: JsObject = Json.obj(
      "limit" -> limit,
      "timeRange" -> Json.arr(
        Json.obj(
          "lowerBound" -> OffsetDateTime.now( ZoneOffset.UTC ).minus(1, ChronoUnit.HOURS).format(DateTimeFormatter.ISO_DATE_TIME) ,
          "upperBound" ->OffsetDateTime.now( ZoneOffset.UTC ).format(DateTimeFormatter.ISO_DATE_TIME)
        )
      ),
      "type" -> "aggregation",
      "aggregationFilter" -> Json.arr(
        Json.obj(
          "operation" -> "avg",
          "value" -> 50,
          "relation" -> ">="
        ),
        Json.obj(
          "operation" -> "max",
        )
      ),
      "filter" -> "tags LIKE 'smartcity' AND measure_name = 'ocupation' ")

    val request = FakeRequest(POST, "/v1/query").withJsonBody(jsonQuery)
    val result: Future[Result] = controller.postQuery.apply(request)

    
    status(result) mustEqual 200
    println(contentAsString(result))
    val size = (contentAsJson(result) \ "items").as[Int]
    assert(size  <= limit)
    if (size > 0) {
      ((contentAsJson(result) \ "series") (0) \ "timestamps").as[Seq[String]].isEmpty mustBe true
      ((contentAsJson(result) \ "series") (0) \ "values").as[Seq[BigDecimal]].isEmpty mustBe true
      (((contentAsJson(result) \ "series") (0) \ "stats") (0) \ "name").as[String] mustBe "avg"
      (((contentAsJson(result) \ "series") (0) \ "stats") (1) \ "name").as[String] mustBe "max"
    }

  }
  "getting correct answer to aggregation query with time series" in {
    val jsonQuery: JsObject = Json.obj(
      "limit" -> limit,
      "timeRange" -> Json.arr(
        Json.obj(
          "lowerBound" -> OffsetDateTime.now( ZoneOffset.UTC ).minus(1, ChronoUnit.HOURS).format(DateTimeFormatter.ISO_DATE_TIME) ,
          "upperBound" ->OffsetDateTime.now( ZoneOffset.UTC ).format(DateTimeFormatter.ISO_DATE_TIME)
        )
      ),
      "timeseries" -> true,
      "type" -> "aggregation",
      "aggregationFilter" -> Json.arr(
        Json.obj(
          "operation" -> "avg",
          "value" -> 50,
          "relation" -> ">="
        ),
        Json.obj(
          "operation" -> "max",
        )
      ),
      "filter" -> "tags LIKE 'smartcity' AND measure_name = 'ocupation' ")

    val request = FakeRequest(POST, "/v1/query").withJsonBody(jsonQuery)
    val result: Future[Result] = controller.postQuery.apply(request)
    val jsonResult = contentAsJson(result)

    status(result) mustEqual 200
    println(contentAsString(result))
    val size = (contentAsJson(result) \ "items").as[Int]
    assert(size  <= limit)
    if (size > 0){
      ((jsonResult \ "series")(0) \ "timestamps").as[Seq[String]].isEmpty mustBe false
      ((jsonResult \ "series")(0) \ "values").as[Seq[BigDecimal]].isEmpty mustBe false
      (((contentAsJson(result) \ "series")(0) \ "stats")(0) \"name").as[String] mustBe "avg"
      (((contentAsJson(result) \ "series")(0) \ "stats")(1) \"name").as[String] mustBe "max"
    }
  }
  "getting correct answer to complex subsequence queries with aggregation" in {
    val subseq = Json.arr(0,1,1,0)
    val jsonQuery: JsObject = Json.obj(
      "limit" -> limit,
      "timeRange" -> Json.arr(
        Json.obj(
          "lowerBound" -> OffsetDateTime.now( ZoneOffset.UTC ).minus(1, ChronoUnit.DAYS).format(DateTimeFormatter.ISO_DATE_TIME) ,
          "upperBound" ->OffsetDateTime.now( ZoneOffset.UTC ).format(DateTimeFormatter.ISO_DATE_TIME)
        )
      ),
      "timeseries" -> true,
      "type" -> "complex",
      "aggregationFilter" -> Json.arr(
        Json.obj(
          "operation" -> "max",
        )
      ),
      "filter" -> "tags LIKE 'smartcity' AND measure_name = 'ocupation' ",
     "subsequenceQuery" -> Json.obj(
       "subsequence" -> subseq
     )
    )
    val request = FakeRequest(POST, "/v1/query").withJsonBody(jsonQuery)
    val result: Future[Result] = controller.postQuery.apply(request)
    val jsonResult = contentAsJson(result)

    println(contentAsString(result))
    status(result) mustEqual 200
    assert((contentAsJson(result) \ "items").as[Int]  <= limit)
    ((jsonResult \ "series")(0) \ "timestamps").as[Seq[String]].isEmpty mustBe false
    val values = ((jsonResult \ "series")(0) \ "values").as[Seq[BigDecimal]]

    values.isEmpty mustBe false
    if (values.size >= subseq.value.size){
      ((jsonResult \ "series")(0) \ "subsequences").as[Seq[Subsequence]].isEmpty mustBe false
    }
    else{
      ((jsonResult \ "series")(0) \ "subsequences").as[Seq[Subsequence]].isEmpty mustBe true
    }
    (((contentAsJson(result) \ "series")(0) \ "stats")(0) \"name").as[String] mustBe "max"
  }
  "getting sql error for wrong filter" in {
    val jsonQuery: JsObject = Json.obj(
      "limit" -> limit,
      "timeRange" -> Json.arr(
        Json.obj(
          "lowerBound" -> OffsetDateTime.now( ZoneOffset.UTC ).minus(1, ChronoUnit.HOURS) ,
          "upperBound" ->OffsetDateTime.now( ZoneOffset.UTC ).format(DateTimeFormatter.ISO_DATE_TIME)
        )
      ),
      "type" -> "simple",
      "filter" -> "tags LIKE 'smartcity' AND measures-names='ocupation' ")

    val request = FakeRequest(POST, "/v1/query").withJsonBody(jsonQuery)
    val result: Future[Result] = controller.postQuery.apply(request)
    println(contentAsString(result))
    status(result) mustEqual 400
    val jsonResult = contentAsJson(result)
    ( jsonResult(0) \ "error").as[String] mustBe "SQL parse failed"
  }


}
