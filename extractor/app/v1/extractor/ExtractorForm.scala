package v1.extractor


import com.typesafe.config.ConfigFactory
import play.api.data.FormError
import play.api.data.validation.{Constraint, Invalid, ValidationResult}
import play.api.libs.json._
import v1.extractor.http.HttpSchemaValidator
import v1.extractor.models.extractor.config.KafkaConfig
import v1.extractor.models.extractor.{DataSchema, MeasureField}
import v1.extractor.models.metadata.{Location, Metadata, Sample, TimeUnit}

case class ExtractorFormInput(extType: String,dataSchema: DataSchema,
                              ioConfig: IOConfigForm, metadata: Metadata)

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
  def validate(extractor: ExtractorFormInput, sensorsToCheck: Int,  maxNumSensor: Int ): ValidationResult
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

  val config = ConfigFactory.defaultApplication().resolve()
  val sensorsToCheck = config.getInt("extractor.sensors-to-check")
  val maxSensors =  config.getInt("extractor.max-sensor-per-extractor")


  /**
   * A form for extractors
   * @param extType type of the extractor
   * @param dataSchema mapping for source
   * @param ioConfig connection config
   */

  val schemaMappingCheck: Constraint[ExtractorFormInput] = Constraint("constraints.schemacheck")({
    extractorInput => {
      ExtractorType.withName(extractorInput.extType) match {
        case ExtractorType.Http =>
          HttpSchemaValidator.validate(extractorInput,
            sensorsToCheck
            , maxSensors
           ) //Numero mágico a sustituir por configuraicón
        case _ => Invalid("Invalid extractor type")
      }

    }
  })
  val maxSizeDescription = 256
  val maxSizeName = 32

  val form = Form(
      mapping(
        "type" -> nonEmptyText.verifying("Bad type",ExtractorType.isExtractorType(_)),
        "dataSchema" -> mapping(
          "sensorIDField" -> nonEmptyText,
          "timestampField" -> nonEmptyText,
          "measures" -> list(
            mapping(
              "name" -> nonEmptyText(maxLength=maxSizeName),
              "field" -> nonEmptyText,
              "unit" -> nonEmptyText,
              "description" -> optional(text(maxLength=maxSizeDescription))
            )(MeasureField.apply)(MeasureField.unapply)
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
        ,"metadata" -> mapping(
          "name" -> nonEmptyText(maxLength=maxSizeName),
          "description" -> optional(text(maxLength=maxSizeDescription)),
          "tags" -> seq(nonEmptyText),
          "sample" -> mapping(
            "freq" -> longNumber(min=1),
            "unit" -> nonEmptyText.verifying(error="Time unit not valid", TimeUnit.isTimeUnit(_))
          )(Sample.apply)(Sample.unapply),
          "localization" -> mapping(
            "name" -> nonEmptyText(maxLength=maxSizeName),
            "address" -> optional(nonEmptyText),
            "city" -> optional(nonEmptyText(maxLength=maxSizeName)),
            "region" -> optional(nonEmptyText(maxLength=maxSizeName)),
            "country" -> optional(nonEmptyText(maxLength=maxSizeName))
          )(Location.apply)(Location.unapply),
          "url" -> optional(text)
        )(Metadata.apply)(Metadata.unapply)
      )(ExtractorFormInput.apply) (ExtractorFormInput.unapply).verifying(schemaMappingCheck)
    )
}
