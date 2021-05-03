package v1.querier

import org.scalatestplus.play.PlaySpec
import v1.querier.models.{AggregationFilter, Query}

class QueryModelTest extends PlaySpec{


  "query model " must {
    "compose well formed queries for aggregation filters" in {
      val basicQuery = new Query(100,List(Tuple2("2021-05-03T08:27:47Z","2021-05-01T08:23:47Z"),
        Tuple2("2021-01-03T08:27:47Z","2021-02-03T08:23:47Z")),"simple","tags = 'smartcity'")

      println(basicQuery.composeBasicQuery)
      val expectedResult = "SELECT * FROM tseriesdb  WHERE (__time >= '2021-05-03T08:27:47Z' AND __time <= '2021-05-01T08:23:47Z') OR (__time >= '2021-01-03T08:27:47Z' AND __time <= '2021-02-03T08:23:47Z') AND tags = 'smartcity' ORDER BY __time"
      basicQuery.composeBasicQuery mustBe expectedResult
    }
    "compose form well formed queries for basic queries" in {
      val aggQuery = new Query(
        100,List(Tuple2("2021-05-03T08:27:47Z","2021-05-03T08:23:47Z")),
        "aggregation", "tags = 'traffic' AND city = 'Santander' AND measure_name = 'ocupation'",
        aggregationFilter = Some(List(AggregationFilter(
          "avg", Some(2.0), relation = Some("<=")),AggregationFilter(
          "min"), AggregationFilter(
          "avg", aggComparation = Some("stddev"), relation = Some("<=")
        ))))

      val expectedQuery = "SELECT seriesID, sensorID, __time, address, city, country, description, measure, measure_desc, measure_name, name, region, sampling_unit, tags, unit,  avg_agg , min_agg  FROM  (SELECT * FROM tseriesdb  WHERE (__time >= '2021-05-03T08:27:47Z' AND __time <= '2021-05-03T08:23:47Z') AND tags = 'traffic' AND city = 'Santander' AND measure_name = 'ocupation' ) INNER JOIN(SELECT seriesID as seriesID2,  avg_agg , min_agg  FROM (SELECT DISTINCT(seriesID), avg(measure) AS avg_agg ,min(measure) AS min_agg  FROM tseriesdb WHERE (__time >= '2021-05-03T08:27:47Z' AND __time <= '2021-05-03T08:23:47Z') AND tags = 'traffic' AND city = 'Santander' AND measure_name = 'ocupation' GROUP BY 1 HAVING avg_agg <= 2.0 AND avg_agg <= (SELECT stddev(measure) AS stddev_agg  FROM tseriesdb  WHERE (__time >= '2021-05-03T08:27:47Z' AND __time <= '2021-05-03T08:23:47Z') AND tags = 'traffic' AND city = 'Santander' AND measure_name = 'ocupation')))ON seriesID = seriesID2 ORDER BY __time"
      aggQuery.composeAggregationQuery mustBe expectedQuery
    }
  }

}
