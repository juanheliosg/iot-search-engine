package v1.extractor


import com.typesafe.config.{Config, ConfigFactory}
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
 * @param inputConfig config for form
 * @param kafkaConfig kafka config
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

  val config: Config = ConfigFactory.defaultApplication().resolve()
  val sensorsToCheck: Int = config.getInt("extractor.sensors-to-check")
  val maxSensors: Int =  config.getInt("extractor.max-sensor-per-extractor")



  val schemaMappingCheck: Constraint[ExtractorFormInput] = Constraint("constraints.schemacheck")({
    extractorInput => {

      val isLatEmpty = extractorInput.dataSchema.latField.isEmpty
      val isLongEmpty = extractorInput.dataSchema.longField.isEmpty
      if ( (!isLatEmpty && isLongEmpty) ||
        ( (isLatEmpty) && (!isLongEmpty))) {
        Invalid("The longitude and latitude fields must be either full or both empty")
      }
      else{
        ExtractorType.withName(extractorInput.extType) match {
          case ExtractorType.Http =>
            HttpSchemaValidator.validate(extractorInput,
              sensorsToCheck
              , maxSensors
            )
          case _ => Invalid("Invalid extractor type")
        }
      }
    }
  })
  val maxSizeDescription = 256
  val maxSizeName = 32

  val form: Form[ExtractorFormInput] = Form(
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
          ).verifying("No measures", _.nonEmpty),
          "latField" -> optional(nonEmptyText),
          "longField" -> optional(nonEmptyText),
        )(DataSchema.apply)(DataSchema.unapply),
        "IOConfig" -> mapping(
          "inputConfig" -> mapping(
            "address" -> nonEmptyText,
            "jsonPath" -> optional(text),
            "freq" -> optional(longNumber(min=1))
            )(InputConfigForm.apply)(InputConfigForm.unapply),
          "kafkaConfig" -> mapping(
            "topic" -> nonEmptyText,
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
