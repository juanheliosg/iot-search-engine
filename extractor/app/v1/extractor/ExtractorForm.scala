package v1.extractor


import play.api.data.FormError
import play.api.data.validation.{Constraint, Invalid, ValidationResult}
import play.api.libs.json.{JsObject, Json}
import v1.extractor.http.HttpSchemaValidator

case class ExtractorFormInput(extType: String,dataSchema: DataSchema,
                              ioConfig: IOConfig)
trait SchemaValidator{
  def validate(extractor: ExtractorFormInput, maxNumSensor: Int ): ValidationResult
}

object ExtractorForm{
  import play.api.data.Form
  import play.api.data.Forms._

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
        "type" -> nonEmptyText.verifying("Bad type",ExtractorType.isExtractorType(_)),
        "dataSchema" -> mapping(
          "sourceID" -> longNumber(min=0),
          "sensorIDField" -> nonEmptyText,
          "timestampField" -> nonEmptyText,
          "measures" -> list(
            mapping(
              "name" -> nonEmptyText,
              "field" -> nonEmptyText
            )(Measure.apply)(Measure.unapply)
          ).verifying("No measures", _.size > 0)
        )(DataSchema.apply)(DataSchema.unapply),
        "IOConfig" -> mapping(
          "address" -> nonEmptyText,
          "jsonPath" -> optional(text),
          "freq" -> optional(longNumber(min=1))
        )(IOConfig.apply)(IOConfig.unapply)
      )(ExtractorFormInput.apply) (ExtractorFormInput.unapply).verifying(schemaMappingCheck)
    )
}
