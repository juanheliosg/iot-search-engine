package v1.querier.models

import v1.querier.models


/**
 * Query to be submited to the system
 *
 * @param limit number of elements to be retrieved
 * @param timeRanges list of time ranges en ISO format to be considered
 * @param queryType type of query can be simple, aggregation and complex
 * @param filter SQL Where clause to filter
 * @param subseQuery Subsequence to be search in the database
 * @param aggregationFilter List of aggregations filters
 * @param tendencyQuery take value asc for filter sensors with ascending series
 * @param order Order the final result using the Order objects in order.
 */
case class Query(limit: Int, timeRanges: List[(String, String)],
                 query: String, filter: String,
                 subseQuery: Option[SubsequenceQuery] = None,
                 aggregationFilter: Option[List[AggregationFilter]] = None,
                 tendencyQuery: Option[String] = None, order: Option[List[Order]] = None){

  val queryType: QueryType.Value = QueryType.withName(query)
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

object OrderFieldType extends ExtendedEnum {
  type OrderFieldType = Value
  val measure_name = Value("measure_name")
  val unit = Value("unit")
  val measure_desc = Value("measure_desc")
  val name = Value("name")
  val city = Value("city")
  val region = Value("region")
  val country = Value("country")
  val address = Value("address")
  val description = Value("description")
  val sampling_unit = Value("sampling_unit")
  val sampling_freq = Value("sampling_freq")
}

/**
 * Object stablishing an order
 * @param field field to order
 * @param asc true for ascending, false for descending
 */
case class Order(field: String, asc: Boolean = true){
  val FieldType = OrderFieldType.withName(field)
}



/**
 * Objeto representando un filtrado y consulta por agregación
 * @param aggreg Tipo de función de agregación a calcular
 * @param value Valor con el que comparar la agregación según la función de relación
 * @param aggComparation Agregación con la que comparar la aggreg original
 * @param relation relación entre la agregación dad y el valor o agregación secundaria
 */
case class AggregationFilter(aggreg: String,
                             value : Option[BigDecimal],
                             aggComparation: Option[String],
                             relation: Option[String]){

  val aggregType = AggregationType.withName(aggreg)

  val aggComparationType: Option[AggregationType.Value] = relation match {
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
  val count = Value("count")

}
object RelationType extends ExtendedEnum {
  type RelationType = Value
  val eq = Value("=")
  val gt = Value(">")
  val lt = Value(">=")
  val ge = Value("<=")

}