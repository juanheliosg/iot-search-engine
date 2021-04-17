package v1.test


import akka.util.Timeout
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

import scala.concurrent.duration.DurationInt


class ExtractorServiceImplTest extends PlaySpec
  with GuiceOneAppPerSuite
  with BeforeAndAfterEach
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
  val schema = new DataSchema(
    2, "ayto:idSensor", "dc:modified",
    List(new Measure("ocupation","ayto:ocupacion",2),
      new Measure("intensity", "ayto:intensidad",1))
  )

  val freq = 3000
  val inputConfig: InputConfigForm = InputConfigForm(sensorAddress, Some("$"), Some(freq))
  val config: IOConfigForm = IOConfigForm(inputConfig, kafkaConfig)




  "getExtractor" must{
    "return correct result if previous extractor has been posted" in {
      val extInput = ExtractorFormInput(
        1, ExtractorType.Http.toString,schema, config)
      val resultPost = service.postExtractor(extInput)
      println(contentAsString(resultPost))
      status(resultPost) mustBe 201 //Created code

      val resultGet = service.getExtractor(extInput.id)
      val content = contentAsJson(resultGet).as[ExtractorGetResponse]
      status(resultGet) mustBe 200 //Ok code
      content.status == "not started" mustBe false
      content.dataSchema mustBe schema
      content.ioConfig.inputConfig mustBe new InputConfig(sensorAddress,Some(new HttpInputConfig(
        "$", freq
      )))
    }
    "fail if no extractor with same id has been posted" in{
      val result = service.getExtractor(2)
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
    "return badRequest if other extractor is posted with same id" in {
      val extInput = ExtractorFormInput(
        2, ExtractorType.Http.toString,schema, config)

      val result = service.postExtractor(extInput)
      status(result) mustBe 201 //Created code

      val resultRepeated = service.postExtractor(extInput)
      status(resultRepeated) mustBe 400 //Bad request
    }
  }
  "putExtractor" must{
    "fail if wrong extractor id" in{
      val extInputPut = ExtractorFormInput(
        23, ExtractorType.Http.toString,schema, config)

      val result = service.updateExtractor(23,extInputPut)
      val expectedResponse = Json.obj(
        "errors" -> Json.arr(
          Json.obj("id" -> "Extractor with provided id does not exists")
        )
      )
      status(result) mustBe 400 //BadRequestcode
    contentAsJson(result) mustBe expectedResponse
    }

    "update extractor if everything ok" in {
      val newId = 5
      val id = 2

      val extInputPost = ExtractorFormInput(
        id, ExtractorType.Http.toString,this.schema, config)

      val resultPost = service.postExtractor(extInputPost)
      println(contentAsString(resultPost))

      val schema = new DataSchema(
        newId, "ayto:idSensor", "dc:modified",
        List(new Measure("ocupation","ayto:ocupacion",2),
          new Measure("intensity", "ayto:intensidad",1))
      )
      val extInput = ExtractorFormInput(
        id, ExtractorType.Http.toString,schema, config)

      val result = service.updateExtractor(id,extInput)
      status(result) mustBe 200

      val resultGet = service.getExtractor(id)
      status(resultGet) mustBe 200

      val content = contentAsJson(resultGet).as[ExtractorGetResponse]

      status(resultGet) mustBe 200 //Ok code
      content.status == "not started" mustBe false
      content.dataSchema.sourceID mustBe newId

    }
  }
  "deleteExtractor" must {
    "delete an extractor if everything correct" in {
      val extInput = ExtractorFormInput(
        2, ExtractorType.Http.toString,schema, config)
      service.postExtractor(extInput)

      val result = service.deleteExtractor(2)
      println(contentAsString(result))
      status(result) mustBe 204 //No content response
    }
  }
  "startExtractor" must {
    "fail if extractor is not stopped" in{
      val id = 222
      val extInput = ExtractorFormInput(
        id, ExtractorType.Http.toString,schema, config)

      val resultPost = service.postExtractor(extInput)
      status(resultPost) mustBe 201


      val expectedResponse = Json.obj(
        "errors" -> Json.arr(
          Json.obj(
            "state" -> "Extractor is not stopped or failed")
        )
      )

      val resultStop = service.startExtractor(id)
      status(resultStop) mustBe 400
      contentAsJson(resultStop) mustBe expectedResponse

    }
    "startExtractor if everything correct" in {
      val id = 221
      val extInput = ExtractorFormInput(
        id, ExtractorType.Http.toString,schema, config)

      val resultPost = service.postExtractor(extInput)
      status(resultPost) mustBe 201

      val resultStop = service.stopExtractor(id)
      status(resultStop) mustBe 200

      val expectedResponse = Json.obj(
        "id" -> id,
        "status" -> "starting")

      val resultStart = service.startExtractor(id)
      status(resultStart) mustBe 200
      contentAsJson(resultStart) mustBe expectedResponse

    }
  }
  "stopExtractor" must{
    "fail if extractor is not running or starting" in{
      val id = 2223

      val resultStop = service.stopExtractor(id)
      val expectedResponse = Json.obj(
        "errors" -> Json.arr(
          Json.obj("state" -> s"Extractor state is not started not running or starting")
        )
      )

      status(resultStop) mustBe 400
      contentAsJson(resultStop) mustBe expectedResponse
    }
    "stopExtractor if everything correct" in{
      val id = 224
      val extInput = ExtractorFormInput(
        id, ExtractorType.Http.toString,schema, config)

      val resultPost = service.postExtractor(extInput)
      status(resultPost) mustBe 201

      val resultStop = service.stopExtractor(id)

      val expectedResponse = Json.obj(
        "id" -> id,
        "status" -> "stopped"
      )
      status(resultStop) mustBe 200
      contentAsJson(resultStop) mustBe expectedResponse
    }
  }

}
