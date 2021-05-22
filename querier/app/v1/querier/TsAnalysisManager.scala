package v1.querier

import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import play.api.Configuration
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{JsPath, Json, Reads}
import play.api.libs.ws.{WSClient, WSRequest}
import v1.querier.models.QueryResponse

import javax.inject.Inject
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

case class SubseqResponse(series_id: String, ed: BigDecimal, start: Int)
object SubseqResponse{
  implicit val tsReader: Reads[SubseqResponse] = Json.reads[SubseqResponse]
}

case class TsAnalysisError(msg: String, `type`: String)
object TsAnalysisError{
  implicit val tsErrorReader: Reads[TsAnalysisError] = (
      (JsPath \ "msg").read[String] and
      (JsPath \ "type").read[String]
    )(TsAnalysisError.apply _)
}
case class TsAnalysisListError(detail: Seq[TsAnalysisError])
object TsAnalysisListError{
  implicit val tsReader: Reads[TsAnalysisListError] = Json.reads[TsAnalysisListError]
}

class TsAnalysisManager @Inject() (config: Configuration, ws: WSClient, implicit val ec: ExecutionContext, implicit val mat: Materializer) extends GeneralAPIManager {
  private val base_url: String = config.underlying.getString("querier.tsanalysis-url")
  private val k_nearest: Int = config.underlying.getInt("querier.k-nearest")
  private val responseChunk: Int = config.underlying.getInt("querier.tsanalysis-chunk")
  private val subseqRequest: WSRequest = ws.url(base_url+"subsequence/search").withRequestTimeout(60.seconds)

  /**
   * Search subsequences in the tsanalysis service using akka streams for chunking requests in order
   * to parallelize the computation.
   * @param series String, QueryResponse tuple
   * @param subseq Subsequence to be analyzed
   * @return A list of SubseResponses
   */
  def searchSubsequence(series: List[(String,QueryResponse)], subseq: List[BigDecimal]): Future[Either[List[SubseqResponse],List[JSONError]]] = {
  //Chunkear los objetos por serie temporal.

    val results = Source(series)
      .grouped(responseChunk)
      .map(series => {
        Json.obj(
          "k_nearest" -> k_nearest,
          "subsequence" -> subseq,
          "time_series" -> series.map( s =>{
              val time_series = s._2
              Json.obj(
                "series_id" ->  time_series.seriesId,
                "sampling_freq"-> time_series.samplingFreq,
                "sampling_unit" -> time_series.samplingUnit,
                "timestamps" -> time_series.timestamps,
                "values" -> time_series.values
              )
            }
            )

        )
      })
      .mapAsync(4)(data => postGeneralQuery[SubseqResponse, TsAnalysisListError](data, subseqRequest,
        JSONError.toErrorList, "results")(SubseqResponse.tsReader, ec, TsAnalysisListError.tsReader))
      .takeWhile( resp => resp match{
        case Left(value) => true
        case Right(value) => false
      },inclusive=true)
      .take(responseChunk)
      .reduce((first, second) => (first,second) match{
        case (Left(firstValue: List[SubseqResponse]),Left(secondValue: List[SubseqResponse])) =>
          Left(firstValue.concat(secondValue))
        case (Left(firstValue),Right(secondValue)) => Right(secondValue)
        case (Right(firstValue),Left(secondValue)) => Right(firstValue)
        case (Right(firstValue),Right(secondValue)) => Right(secondValue)
      })
      .recover{
        case t: Throwable =>
          Right(List(JSONError("Error processing subsequence responses", t.getMessage)))
      }
      .runWith(Sink.head)(mat)

    results.map{
      case Left(value) =>
        if (value.nonEmpty)
            Left(value)
        else
          Left(List.empty)
      case Right(value) => Right(value)
    }



  }


}


