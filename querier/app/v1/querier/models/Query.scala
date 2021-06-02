package v1.querier.models

import com.typesafe.config.{Config, ConfigFactory}
import v1.querier.models
import v1.querier.models.Query.datasource

object Query{
  val config: Config = ConfigFactory.defaultApplication().resolve()
  val datasource = config.getString("querier.datasource")
}

/**
 * Query to be submited to the system
 *
 * @param limit number of elements to be retrieved
 * @param timeseries true for returning the time series values, false for just the series metadata
 * @param timeRanges list of time ranges en ISO formatForm to be considered
 * @param query type of query can be simple, aggregation and complex
 * @param filter SQL Where clause to filter
 * @param subseQuery Subsequence to be search in the database
 * @param aggregationFilter List of aggregations filters
 * @param tendencyQuery take value asc for filter sensors with ascending series
 */
case class Query(limit: Int, timeRanges: List[(String, String)],
                 timeseries: Boolean = false,
                 query: String, filter: String,
                 subseQuery: Option[SubsequenceQuery] = None,
                 aggregationFilter: Option[List[AggregationFilter]] = None){

  val queryType: QueryType.Value = QueryType.withName(query)

  /**
   * Compose where clausule with intervals and filters
   * @return
   */
  def composeWhere(): String = {
    val intervalQuery = timeRanges.map( range => {
      s"(__time >= '${range._1}' AND __time <= '${range._2}')"
    }).mkString(" OR ")

    s" WHERE $intervalQuery AND $filter"
  }

  /**
   * Compose a basic query using intervals and filters
   * @return
   */
  def composeBasicQuery: String = {
    if (timeseries){
      s"SELECT seriesID, sensorID, measure, __time, address, city, country, description, " +
        "measure_desc, measure_name, name, region, sampling_unit, sampling_freq," +
        s" tags, unit, lat, long FROM $datasource ${composeWhere()}"
    }
    else{
      s"SELECT DISTINCT seriesID, sensorID, address, city, country, description, " +
        "measure_desc, measure_name, name, region, sampling_unit, sampling_freq," +
        s" tags, unit, lat, long FROM $datasource ${composeWhere()}"
    }
  }

  /**
   * Compose an aggregation query
   * agg
   * @return
   */
  def composeAggregationQuery: String = {
    val aggFilter = aggregationFilter.get
    val aggResults = aggFilter.map(agg => s" ${agg.aggreg}_agg ").toSet.mkString(",")
    val whereClausule = composeWhere()

    val selection = timeseries match{
      case true =>  s"SELECT seriesID, sensorID, __time, address, city, country, description, " +
        "measure, measure_desc, measure_name, name, region, sampling_unit, sampling_freq," +
        s" tags, unit, lat, long, $aggResults FROM "
      case false => s"SELECT DISTINCT seriesID, sensorID, address, city, country, description, " +
        "measure_desc, measure_name, name, region, sampling_unit, sampling_freq," +
        s" tags, unit, lat, long, $aggResults FROM "
    }


    val aggComputation = aggFilter.map( filter =>{
        val agg = filter.aggreg
        s"$agg(measure) AS ${agg}_agg "
    }
    ).toSet.mkString(",")

    val havingClausule = aggFilter.map(filter => {

      if (filter.value.nonEmpty){
        s"${filter.aggreg}_agg ${filter.relation.get} ${filter.value.get}"
      }
      else if(filter.aggComparation.nonEmpty){
        s" ${filter.aggreg}_agg ${filter.relation.get} " +
          s"(SELECT ${
            val agg = filter.aggComparation.get
            s"$agg(measure) AS ${agg}_agg "
          } FROM $datasource $whereClausule)"
      }
      else{
        Nil
      }
    }).filter(_ != Nil).mkString(" AND ")

    if (timeseries){
      s"$selection (SELECT * FROM $datasource $whereClausule )" +
        s" INNER JOIN" +
        s"(SELECT seriesID as seriesID2, $aggResults FROM " +
        s"(SELECT DISTINCT(seriesID), $aggComputation FROM $datasource" +
        s"$whereClausule GROUP BY 1 ${if (havingClausule.nonEmpty) s"HAVING $havingClausule" else ""}))" +
        s"ON seriesID = seriesID2"
    }
    else{
      s"$selection (SELECT * FROM $datasource $whereClausule )" +
        s" INNER JOIN" +
        s"(SELECT seriesID as seriesID2, $aggResults FROM " +
        s"(SELECT DISTINCT(seriesID), $aggComputation FROM $datasource" +
        s"$whereClausule GROUP BY 1 ${if (havingClausule.nonEmpty) s"HAVING $havingClausule"}))" +
        s"ON seriesID = seriesID2"

    }


  }
}

object QueryType extends ExtendedEnum {
  type QueryType = Value
  val simple: models.QueryType.Value = Value("simple")
  val aggregation: models.QueryType.Value = Value("aggregation")
  val complex: models.QueryType.Value = Value("complex")
}


/**
 * Subsequence query object
 * @param subsequence list of bigdecimals corresponding to the subsequence to be search.
 * @param normalization true if normalized subsequence search false for non-normalized search
 * @param equality true for searching the most similar subsequences, false for search for the more different
 */
case class SubsequenceQuery(subsequence: List[BigDecimal],
                            normalization: Boolean = true,
                            equality: Boolean = true)






/**
 * Objeto representando un filtrado y consulta por agregación
 * @param aggreg Tipo de función de agregación a calcular
 * @param value Valor con el que comparar la agregación según la función de relación
 * @param aggComparation Agregación con la que comparar la aggreg original
 * @param relation relación entre la agregación dad y el valor o agregación secundaria
 */
case class AggregationFilter(aggreg: String,
                             value : Option[BigDecimal]=None,
                             aggComparation: Option[String]=None,
                             relation: Option[String] = None){

  val aggregType = AggregationType.withName(aggreg)

  val aggComparationType: Option[AggregationType.Value] = aggComparation match {
    case Some(value) => Some(AggregationType.withName(value))
    case None => None
  }
  val relationType: Option[RelationType.Value] = relation match {
    case Some(value) => Some(RelationType.withName(value))
    case None => None
  }
}
object AggregationType extends ExtendedEnum {
  type AggregationType = Value
  val avg = Value("avg")
  val std = Value("stddev")
  val sum = Value("sum")
  val max = Value("max")
  val min = Value("min")
}

object RelationType extends ExtendedEnum {
  type RelationType = Value
  val eq = Value("=")
  val gt = Value(">")
  val le = Value("<")
  val lt = Value(">=")
  val ge = Value("<=")

}
