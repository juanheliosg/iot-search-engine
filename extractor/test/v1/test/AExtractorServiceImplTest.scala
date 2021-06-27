package v1.test


import akka.util.Timeout
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.testcontainers.containers.{CassandraContainer, KafkaContainer}
import org.testcontainers.utility.DockerImageName
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers.{contentAsJson, contentAsString, status}
import v1.extractor._
import v1.extractor.models.extractor.config.{HttpInputConfig, InputConfig, KafkaConfig}
import v1.extractor.models.extractor.{DataSchema, ExtractorGetResponse, MeasureField}
import v1.extractor.models.metadata.{Location, Metadata, Sample}

import scala.concurrent.duration.DurationInt


class ExtractorServiceImplTest extends PlaySpec
  with GuiceOneAppPerSuite
  with BeforeAndAfterEach
  with MockitoSugar
{
  implicit val timeout: Timeout = Timeout(60.seconds)
  //Cassandra container is shared between all tests
  val cassandraContainer = new CassandraContainer("cassandra")
  cassandraContainer.start()
  val port: Integer = cassandraContainer.getMappedPort(9042)

  private val kafkaContainer = new KafkaContainer(
    DockerImageName.parse("confluentinc/cp-kafka:latest"))
  kafkaContainer.start()
  private val kafkaAddress = kafkaContainer.getBootstrapServers
  private val kafkaConfig = new KafkaConfig("test", kafkaAddress)

//Application builder con custom datastax-java-driver
  override def fakeApplication(): Application = {
    //Override config for getting cassandra container port (testcontainer randomize ports)
    GuiceApplicationBuilder().configure(
      Map("datastax-java-driver.basic.contact-points"
        -> List(s"localhost:$port"),
        "extractor.timeout-seconds" -> 50,
        "extractor.max-sensor-per-extractor" -> 10000,
        "extractor.sensor-to-check" -> 5,
        "datastax-java-driver.basic.load-balancing-policy.local-datacenter" -> "datacenter1",
        "akka.persistence.cassandra.journal.keyspace-autocreate" -> "on",
        "akka.persistence.cassandra.journal.tables-autocreate" -> "on",
      "akka.persistence.cassandra.snapshot.keyspace-autocreate" -> "on",
    "akka.persistence.cassandra.snapshot.tables-autocreate" -> "on"
      ))
      .build()
}

  val service: ExtractorServiceImpl = app.injector.instanceOf[ExtractorServiceImpl]

  val sensorAddress =  "https://run.mocky.io/v3/c4017730-abcc-43ac-b28e-5292ddddc9e8"
  val schema = new DataSchema("ayto:idSensor", "dc:modified", List(new MeasureField("ocupation","ayto:ocupacion","veh/h",Some("Vehículos por hora sobre una espiga")),
    new MeasureField("intensity", "ayto:intensidad","%")))

  val freq = 3000
  val metadata: Metadata = Metadata("traffic-santander",Some("Santander traffic flow sensors"),Seq("traffic","static"),
    new Sample(1,"seconds"),
    new Location("santander city",city=Some("Santander"),region=Some("Santander"),country=Some("Spain")),
    url = Some("http://datos.santander.es/dataset/?id=datos-trafico"))

  val inputConfig: InputConfigForm = InputConfigForm(sensorAddress, Some("$"), Some(freq))
  val config: IOConfigForm = IOConfigForm(inputConfig, kafkaConfig)



  "getExtractor" must{
    "return correct result if previous extractor has been posted" in {
      val extInput = ExtractorFormInput( ExtractorType.Http.toString,schema, config, metadata)
      val resultPost = service.postExtractor(extInput)
      val response = contentAsJson(resultPost)
      val respId = (response \ "id").as[String]

      status(resultPost) mustBe 201 //Created code


      val resultGet = service.getExtractor(respId)
      val content = contentAsJson(resultGet).as[ExtractorGetResponse]
      status(resultGet) mustBe 200 //Ok code
      content.status == "not started" mustBe false
      content.dataSchema mustBe schema
      content.ioConfig.inputConfig mustBe new InputConfig(sensorAddress,Some(new HttpInputConfig(
        "$", freq
      )))
    }
    "fail if no extractor with same id has been posted" in{
      val result = service.getExtractor("2")
      val expectedResponse = Json.obj(
        "errors" -> Json.arr(
          Json.obj("id" -> "Extractor with provided id does not exists")
        )
      )
      status(result) mustBe 400
      contentAsJson(result) mustBe expectedResponse

    }
  }

  "postExtractor" must {
    "fail if several collisions are repeated" in {
      val spyService = spy(service)
      val inputConfig2: InputConfigForm = InputConfigForm("fakeaddress", Some("$"), Some(freq))
      val config: IOConfigForm = IOConfigForm(inputConfig2, kafkaConfig)

      val extInput2 = ExtractorFormInput(ExtractorType.Http.toString,schema, config, metadata)

      val result = service.postExtractor(extInput2)
      val response = contentAsJson(result)
      val respId = (response \ "id").as[String]

      when(spyService.generateUniqueId()).thenReturn(respId)
      status(result) mustBe 201 //Created code

      val inputConfig3: InputConfigForm = InputConfigForm("fakead13d2ress", Some("$"), Some(freq))
      val config3: IOConfigForm = IOConfigForm(inputConfig3, kafkaConfig)

      val extInput3 = ExtractorFormInput(ExtractorType.Http.toString,schema, config3, metadata)

      val resultRepeated = spyService.postExtractor(extInput3)

      status(resultRepeated) mustBe 500
    }
  }
  "putExtractor" must{
    "fail if wrong extractor id" in{
      val inputConfig: InputConfigForm = InputConfigForm("fakeadd2ress", Some("$"), Some(freq))
      val config: IOConfigForm = IOConfigForm(inputConfig, kafkaConfig)
      val extInputPut = ExtractorFormInput(ExtractorType.Http.toString,schema, config, metadata)

      val result = service.updateExtractor("2",extInputPut)
      val expectedResponse = Json.obj(
        "errors" -> Json.arr(
          Json.obj("id" -> "Extractor with provided id does not exists")
        )
      )
      status(result) mustBe 400 //BadRequestcode
    contentAsJson(result) mustBe expectedResponse
    }

    "update extractor if everything ok" in {
      val newIdField = "Ayto:newfield"

      val inputConfig: InputConfigForm = InputConfigForm("fakead13d2r2ess", Some("$"), Some(freq))
      val config1: IOConfigForm = IOConfigForm(inputConfig, kafkaConfig)
      val extInputPost = ExtractorFormInput(ExtractorType.Http.toString,this.schema, config1, metadata)

      val resultPost = service.postExtractor(extInputPost)
      val response = contentAsJson(resultPost)
      val respId = (response \ "id").as[String]

      val schema = new DataSchema(newIdField, "dc:modified", List(new MeasureField("ocupation","ayto:ocupacion","veh/h",Some("Vehículos por hora sobre una espiga")),
        new MeasureField("intensity", "ayto:intensidad","%",None)))
      val extInput = ExtractorFormInput(ExtractorType.Http.toString,schema, config1, metadata)

      val result = service.updateExtractor(respId,extInput)
      status(result) mustBe 200

      val resultGet = service.getExtractor(respId)
      status(resultGet) mustBe 200

      val content = contentAsJson(resultGet).as[ExtractorGetResponse]

      status(resultGet) mustBe 200 //Ok code
      content.status == "not started" mustBe false
      content.dataSchema.sensorIDField mustBe newIdField

    }
  }
  "deleteExtractor" must {
    "delete an extractor if everything correct" in {
      val inputConfig: InputConfigForm = InputConfigForm("fakead13d2redeletess", Some("$"), Some(freq))
      val config: IOConfigForm = IOConfigForm(inputConfig, kafkaConfig)

      val extInput = ExtractorFormInput(ExtractorType.Http.toString,schema, config, metadata)
      val resultPost = service.postExtractor(extInput)

      val response = contentAsJson(resultPost)
      val respId = (response \ "id").as[String]

      val result = service.deleteExtractor(respId)
      println(contentAsString(result))
      status(result) mustBe 204 //No content response
    }
  }
  "startExtractor" must {
    "fail if extractor is not stopped" in{
      val id = 222
      val inputConfig: InputConfigForm = InputConfigForm("fakead13d2r2ess111", Some("$"), Some(freq))
      val config1: IOConfigForm = IOConfigForm(inputConfig, kafkaConfig)

      val extInput = ExtractorFormInput(ExtractorType.Http.toString,schema, config1, metadata)

      val resultPost = service.postExtractor(extInput)
      status(resultPost) mustBe 201
      val response = contentAsJson(resultPost)
      val respId = (response \ "id").as[String]


      val expectedResponse = Json.obj(
        "errors" -> Json.arr(
          Json.obj(
            "state" -> "Extractor is not stopped or failed")
        )
      )

      val resultStop = service.startExtractor(respId)
      status(resultStop) mustBe 400
      contentAsJson(resultStop) mustBe expectedResponse

    }
    "startExtractor if everything correct" in {
      val inputConfig: InputConfigForm = InputConfigForm("fake113d2r2ess", Some("$"), Some(freq))
      val config1: IOConfigForm = IOConfigForm(inputConfig, kafkaConfig)

      val extInput = ExtractorFormInput(ExtractorType.Http.toString,schema, config1, metadata)

      val resultPost = service.postExtractor(extInput)
      val response = contentAsJson(resultPost)
      val respId = (response \ "id").as[String]
      status(resultPost) mustBe 201

      val resultStop = service.stopExtractor(respId)
      status(resultStop) mustBe 200

      val expectedResponse = Json.obj(
        "id" -> respId,
        "status" -> "starting")

      val resultStart = service.startExtractor(respId)
      status(resultStart) mustBe 200
      contentAsJson(resultStart) mustBe expectedResponse

    }
  }

  "stopExtractor" must {
    "fail if extractor is not running or starting" in {
      val id = "2223"

      val resultStop = service.stopExtractor(id)
      val expectedResponse = Json.obj(
        "errors" -> Json.arr(
          Json.obj("state" -> s"Extractor state is not started not running or starting")
        )
      )

      status(resultStop) mustBe 400
      contentAsJson(resultStop) mustBe expectedResponse
    }
    "stopExtractor if everything correct" in {
      val inputConfig: InputConfigForm = InputConfigForm("fakead13d122123r2ess", Some("$"), Some(freq))
      val config1: IOConfigForm = IOConfigForm(inputConfig, kafkaConfig)

      val extInput = ExtractorFormInput(ExtractorType.Http.toString, schema, config1, metadata)

      val resultPost = service.postExtractor(extInput)
      status(resultPost) mustBe 201
      val response = contentAsJson(resultPost)
      val respId = (response \ "id").as[String]

      val resultStop = service.stopExtractor(respId)

      val expectedResponse = Json.obj(
        "id" -> respId,
        "status" -> "stopped"
      )
      status(resultStop) mustBe 200
      contentAsJson(resultStop) mustBe expectedResponse
    }
  }
    "getAllExtractors if everything correct" in {


      val inputConfig: InputConfigForm = InputConfigForm("fakead13d2r2essssssss", Some("$"), Some(freq))
      val config1: IOConfigForm = IOConfigForm(inputConfig, kafkaConfig)
      val extInput = ExtractorFormInput(ExtractorType.Http.toString,schema, config1, metadata)
      val resp = service.postExtractor(extInput)
      status(resp) mustBe 201

      val addressSingleSource = "https://run.mocky.io/v3/f6abb769-7fc6-4e19-9313-e09096de138c"
      val inputConfig2: InputConfigForm = InputConfigForm(addressSingleSource, Some("$"), Some(freq))
      val config2: IOConfigForm = IOConfigForm(inputConfig2, kafkaConfig)

      val extInput2 = ExtractorFormInput(ExtractorType.Http.toString,schema, config2, metadata)
      val resp2 = service.postExtractor(extInput2)
      status(resp2) mustBe 201

      val addressJsonPath = "https://run.mocky.io/v3/dec8605f-60b5-4517-bcba-427ac5e316f4"
      val inputConfig3: InputConfigForm = InputConfigForm(addressJsonPath, Some("$.resources"), Some(freq))
      val config3: IOConfigForm = IOConfigForm(inputConfig3, kafkaConfig)

      val extInput3 = ExtractorFormInput(ExtractorType.Http.toString,schema, config3, metadata)
      val resp3 = service.postExtractor(extInput3)
      status(resp3) mustBe 201

      val result = service.getAllExtractors(100)
      status(result) mustBe 200

      assert((contentAsJson(result) \ "items").as[Int] >= 3) //Single test contains 3 extractors full test 10
    }

}
