package v1.extractor


import play.api.data.FormError
import play.api.data.validation.{Constraint, Invalid, ValidationResult}
import play.api.libs.json._
import v1.extractor.http.HttpSchemaValidator

case class ExtractorFormInput(id: Long, extType: String,dataSchema: DataSchema,
                              ioConfig: IOConfigForm)

/**
 * Configuration form
 * @param address uri direction for retrieving sensor data
 * @param jsonPath optional jsonPATH for selecting
 * @param freq optional retrieving frequency
 */
case class InputConfigForm(address: String, jsonPath: Option[String], freq: Option[Long])
object InputConfigForm{
  implicit val format: Format[InputConfigForm] = Json.format
}

/**
 * InputConfig and Kafka config form
 * @param inputConfig
 * @param kafkaConfig
 */
case class IOConfigForm(inputConfig: InputConfigForm, kafkaConfig: KafkaConfig)
object IOConfigForm{
  implicit val format: Format[IOConfigForm] = Json.format
}

/**
 * Interface that all schema validators must implement
 */
trait SchemaValidator{
  def validate(extractor: ExtractorFormInput, maxNumSensor: Int ): ValidationResult
}

/**
 * Extractor generic form class.
 */
object ExtractorForm{
  import play.api.data.Form
  import play.api.data.Forms._
  /**
   * Returns a JsObject with custom format for errors
   * general key is for errors not related with fields but with the entire extractor
   * @param errors
   * @return
   */
  def formatErrorMessage(errors: Seq[FormError]) : JsObject= {
    Json.obj{
      "errors" -> errors.map( error => {
        Json.obj(
        if (error.key.isEmpty) {
          "general" -> error.message
        } else
          error.key -> error.message
        )
      })
    }
  }

  /**
   * A form for extractors
   * @param extType type of the extractor
   * @param dataSchema mapping for source
   * @param ioConfig connection config
   */

  val schemaMappingCheck: Constraint[ExtractorFormInput] = Constraint("constraints.schemacheck")({
    extractorInput => {
      ExtractorType.withName(extractorInput.extType) match {
        case ExtractorType.Http => HttpSchemaValidator.validate(extractorInput,10000) //Numero mágico a sustituir por configuraicón
        case _ => Invalid("Invalid extractor type")
      }
    }
  })

  val form = Form(
      mapping(
        "id" -> longNumber(min=0),
        "type" -> nonEmptyText.verifying("Bad type",ExtractorType.isExtractorType(_)),
        "dataSchema" -> mapping(
          "sourceID" -> longNumber(min=0),
          "sensorIDField" -> nonEmptyText,
          "timestampField" -> nonEmptyText,
          "measures" -> list(
            mapping(
              "name" -> nonEmptyText,
              "field" -> nonEmptyText,
              "measureID" -> longNumber(min=0)
            )(Measure.apply)(Measure.unapply)
          ).verifying("No measures", _.size > 0)
        )(DataSchema.apply)(DataSchema.unapply),
        "IOConfig" -> mapping(
          "inputConfig" -> mapping(
            "address" -> nonEmptyText,
            "jsonPath" -> optional(text),
            "freq" -> optional(longNumber(min=1))
            )(InputConfigForm.apply)(InputConfigForm.unapply),
          "kafkaConfig" -> mapping(
            "topic" -> nonEmptyText, //Aquí no se checkea la validez del server
            "server" -> nonEmptyText
          )(KafkaConfig.apply)(KafkaConfig.unapply)
        )(IOConfigForm.apply)(IOConfigForm.unapply)
      )(ExtractorFormInput.apply) (ExtractorFormInput.unapply).verifying(schemaMappingCheck)
    )
}
