package v1.querier

import v1.querier.models._

import java.time.format.DateTimeFormatter


object QueryForm {
  import play.api.data.Form
  import play.api.data.Forms._

  /**
   * Check if a date is in ISO formatForm
   * @param date date to be parse
   * @return
   */
  private def isISO(date: String): Boolean = {
    try {
      DateTimeFormatter.ISO_DATE_TIME.parse(date)
      true
    }
    catch {
      case _: Throwable =>
        false
    }
  }

  val form: Form[Query] = Form(
    mapping(
      "limit" -> number(min=0),
      "timeRange" -> list(
          tuple(
            "lowerBound" -> nonEmptyText.verifying("Date is not in ISO formatForm",isISO(_)),
            "upperBound" -> nonEmptyText.verifying("Date is not in ISO formatForm",isISO(_))
          )
        ).verifying("Empty time range list",_.nonEmpty),
      "timeseries"-> default(boolean,false),
      "type" -> nonEmptyText.verifying("Invalid query type", QueryType.isType(_)),
      "filter" -> text,
      "subsequenceQuery" -> optional(
        mapping(
        "subsequence" -> list(bigDecimal).verifying("Subsequence must be greater than 2",_.size > 2),
        "normalization" -> default(boolean, true),
        "equality" -> default(boolean, true),
      )(SubsequenceQuery.apply)(SubsequenceQuery.unapply)),

      "aggregationFilter" -> optional(list(
          mapping(
          "operation" -> nonEmptyText.verifying(AggregationType.isType(_)),
            "value" -> optional(bigDecimal),
            "aggComparation" -> optional(nonEmptyText.verifying(AggregationType.isType(_))),
            "relation" -> optional(nonEmptyText.verifying(RelationType.isType(_)))
          )(AggregationFilter.apply)(AggregationFilter.unapply).verifying(
            "Value and aggComparation are mutually exclusive. Pick one or another but not both",
            aggFilter => (aggFilter.value.isEmpty && aggFilter.aggComparation.nonEmpty) ||
                        (aggFilter.value.nonEmpty && aggFilter.aggComparation.isEmpty) ||
                        (aggFilter.value.isEmpty && aggFilter.aggComparation.isEmpty)
          )
        )
      )
    )(Query.apply)(Query.unapply)
      .verifying("Complex queries must have timeseries set to true",q => q.subseQuery match{
        case None => !q.timeseries || q.query != QueryType.complex
        case _ => q.timeseries
      })
      .verifying("Aggregation queries must have aggregationFilter field", q=> {
        val qtype = QueryType.withName(q.query)
        q.aggregationFilter match{
          case None =>
            qtype != QueryType.aggregation
          case _ =>
            qtype == QueryType.aggregation || qtype == QueryType.complex
        }
      })
  )

}
