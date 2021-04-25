package v1.test

import akka.Done
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.ActorRef
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.pattern.StatusReply
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import akka.persistence.typed.PersistenceId
import akka.stream.scaladsl.Sink
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import org.scalatest.wordspec.AnyWordSpecLike
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName
import play.api.Configuration
import v1.extractor.actors.ExtractorGuardianEntity.{Status, Summary}
import v1.extractor.actors.{ExtractorGuardianEntity, PlayActorConfig}
import v1.extractor._
import v1.extractor.models.extractor.config.{HttpInputConfig, InputConfig, KafkaConfig}
import v1.extractor.models.extractor.{DataSchema, ExtractorState, IOConfig, MeasureField}
import v1.extractor.models.metadata.{Location, Metadata, Sample}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object ExtractorTestSpec{
    lazy val config: Config = ConfigFactory.empty()
    .withFallback(EventSourcedBehaviorTestKit.config)
      .withFallback(ConfigFactory.parseString(
        """
          akka.actor.serialization-bindings {
          "v1.extractor.actors.CborSerializable" = jackson-cbor
          }
          extractor.max-sensor-per-extractor = 100000
          extractor.timeout-seconds = 10
          """
      ))
    .resolve()
}


class ExtractorActorTest extends ScalaTestWithActorTestKit(ExtractorTestSpec.config)
  with AnyWordSpecLike
  with BeforeAndAfterEach
  with MockitoSugar{

  val shardMock: ActorRef[ClusterSharding.ShardCommand] = mock[ActorRef[ClusterSharding.ShardCommand]]
  val contextMock: ActorRef[ExtractorGuardianEntity.Command] = mock[ActorRef[ExtractorGuardianEntity.Command]]
  when(shardMock.tell(ClusterSharding.Passivate(contextMock)))

  private val playActorConfig = new PlayActorConfig(this.testKit.internalSystem.classicSystem, new Configuration(this.testKit.config))
  private val eventSourcedTestKit =
    EventSourcedBehaviorTestKit[ExtractorGuardianEntity.Command,
      ExtractorGuardianEntity.Event, ExtractorGuardianEntity.ExtractorGuardian] (system,
      ExtractorGuardianEntity("1", shardMock,PersistenceId.of(ExtractorGuardianEntity.TypeKey.name,"1")
        , playActorConfig))

  //Sensor source mocking
  private val sensorAddress =  "https://run.mocky.io/v3/c4017730-abcc-43ac-b28e-5292ddddc9e8"
  private val schema = new DataSchema("ayto:idSensor", "dc:modified",
    List(new MeasureField("ocupation","ayto:ocupacion","veh/h",Some("Vehículos por hora sobre una espiga")),
        new MeasureField("intensity", "ayto:intensidad","%",None)))

  private val kafkaContainer = new KafkaContainer(
    DockerImageName.parse("confluentinc/cp-kafka:latest"))
  kafkaContainer.start()
  private val kafkaAddress = kafkaContainer.getBootstrapServers
  private val kafkaConfig = new KafkaConfig("test", kafkaAddress)

  private val freq = 3000
  val inputConfig: InputConfig = InputConfig(sensorAddress,
    Some( HttpInputConfig("$", freq)))
  val config: IOConfig = IOConfig(inputConfig, kafkaConfig)

  val metadata = Metadata("traffic-santander",Some("Santander traffic flow sensors"),Seq("traffic","static"),
    new Sample(1,"seconds"),
    new Location("santander city",city=Some("Santander"),region=Some("Santander"),country=Some("Spain")),
    url = Some("http://datos.santander.es/dataset/?id=datos-trafico"))

  val extData: ExtractorState = ExtractorState(schema,config,ExtractorType.Http, metadata)


  override protected def beforeEach(): Unit = {
    super.beforeEach()
    eventSourcedTestKit.clear()
  }
  override protected def afterAll(): Unit = {
    super.afterAll()
    kafkaContainer.stop()
  }

  def waitUntilStarted = {
    var result = eventSourcedTestKit.runCommand[StatusReply[Status]]( ref => ExtractorGuardianEntity.getStatus(ref))
    var isStarting = result.reply.getValue.status == "starting"
    while (isStarting){
      result = eventSourcedTestKit.runCommand[StatusReply[Status]]( ref => ExtractorGuardianEntity.getStatus(ref))
      isStarting = result.reply.getValue.status == "starting"
    }
    result
  }

  "Guardian extractor actor" must  {
    "be created with empty state" in{
      val result = eventSourcedTestKit
        .runCommand[StatusReply[Status]]( ref => ExtractorGuardianEntity.getStatus(ref))
      result.reply.getValue.status shouldBe "not started"
      result.state shouldBe ExtractorGuardianEntity.NotStartedExtractor("1")
    }
    "fail when starting with empty state" in {
      val result = eventSourcedTestKit
        .runCommand[StatusReply[Done]](ref => ExtractorGuardianEntity.startExtractor(ref))
      result.reply shouldBe StatusReply.Error("Extractor state is empty. Update state for starting the extractor")

    }
    "fail when getting extractor with empty state" in {
      val result = eventSourcedTestKit
        .runCommand[StatusReply[Summary]](ref => ExtractorGuardianEntity.getExtractor(ref))
      result.reply shouldBe StatusReply.Error("Extractor state is empty")

    }
    "updateExtractor and start stream" in {
      val result = eventSourcedTestKit.runCommand[StatusReply[Summary]](ref => ExtractorGuardianEntity.updateExtractor(extData, ref))

      result.reply.getValue.extractorState shouldBe extData
      result.reply.getValue.status.status shouldBe "not started"
      result.event shouldBe ExtractorGuardianEntity.extractorUpdated(extData)

      val result2 = eventSourcedTestKit
        .runCommand[StatusReply[Done]](ref => ExtractorGuardianEntity.startExtractor(ref))

      result2.reply shouldBe StatusReply.Ack
      result2.stateOfType[ExtractorGuardianEntity.StartingExtractor].state shouldBe "starting"
    }
    "handle getExtractor" in {
      eventSourcedTestKit
        .runCommand[StatusReply[Summary]](ref => ExtractorGuardianEntity.updateExtractor(extData, ref))
      val result = eventSourcedTestKit
        .runCommand[StatusReply[Summary]]( ref => ExtractorGuardianEntity.getExtractor(ref))

      result.reply.getValue.extractorState shouldBe extData
      result.hasNoEvents shouldBe true
    }
    "pass to failed state if stream fails" in{
      val addressFormatChanged = "https://run.mocky.io/v3/9f143a4f-b194-40a3-9fae-0f6bd215c018"
      val inputConfig = InputConfig(addressFormatChanged,
        Some(HttpInputConfig("$", 3000)))
      val config = IOConfig(inputConfig, kafkaConfig)
      val extData = ExtractorState(schema,config,ExtractorType.Http, metadata)
      eventSourcedTestKit.runCommand[StatusReply[Summary]](ref => ExtractorGuardianEntity.updateExtractor(extData, ref))
      eventSourcedTestKit.runCommand[StatusReply[Done]](ref => ExtractorGuardianEntity.startExtractor(ref))

      val result = waitUntilStarted

      result.stateOfType[ExtractorGuardianEntity.FailedExtractor].state shouldBe "error"
    }
    "pass to failed state if wrong kafka config" in {
      val kafkaConfig = new KafkaConfig("xd","localfail")
      val config = IOConfig(inputConfig, kafkaConfig)
      val extData = ExtractorState(schema,config,ExtractorType.Http, metadata)

      eventSourcedTestKit.runCommand[StatusReply[Summary]](ref => ExtractorGuardianEntity.updateExtractor(extData, ref))
      eventSourcedTestKit.runCommand[StatusReply[Done]](ref => ExtractorGuardianEntity.startExtractor(ref))


      val result = waitUntilStarted
      result.stateOfType[ExtractorGuardianEntity.FailedExtractor].state shouldBe "error"
    }

    "pass to Running state if stream is working" in{

      eventSourcedTestKit.runCommand[StatusReply[Summary]](ref => ExtractorGuardianEntity.updateExtractor(extData, ref))
      eventSourcedTestKit.runCommand[StatusReply[Done]](ref => ExtractorGuardianEntity.startExtractor(ref))

      val result = waitUntilStarted

      result.stateOfType[ExtractorGuardianEntity.RunningExtractor].state shouldBe "running"
    }
    "restart failed extractor with start command" in {
      val kafkaConfig = new KafkaConfig("xd","localfail")
      val config = IOConfig(inputConfig, kafkaConfig)
      val extData = ExtractorState(schema,config,ExtractorType.Http, metadata)

      eventSourcedTestKit.runCommand[StatusReply[Summary]](ref => ExtractorGuardianEntity.updateExtractor(extData, ref))
      eventSourcedTestKit.runCommand[StatusReply[Done]](ref => ExtractorGuardianEntity.startExtractor(ref))

      val result = waitUntilStarted
      result.stateOfType[ExtractorGuardianEntity.FailedExtractor].state shouldBe "error"
      val result2 = eventSourcedTestKit.runCommand[StatusReply[Done]](ref => ExtractorGuardianEntity.startExtractor(ref))
      result2.stateOfType[ExtractorGuardianEntity.StartingExtractor].state shouldBe "starting"

    }

    "handle stopExtractor passing to stopped state and then restarting again" in {
      eventSourcedTestKit.runCommand[StatusReply[Summary]](ref => ExtractorGuardianEntity.updateExtractor(extData, ref))
      eventSourcedTestKit.runCommand[StatusReply[Done]](ref => ExtractorGuardianEntity.startExtractor(ref))
      val result = eventSourcedTestKit.runCommand[StatusReply[Done]](ref => ExtractorGuardianEntity.stopExtractor(ref))
      result.stateOfType[ExtractorGuardianEntity.StoppedExtractor].state shouldBe "stopped"

      val result2 = eventSourcedTestKit.runCommand[StatusReply[Done]](ref => ExtractorGuardianEntity.startExtractor(ref))
      result2.stateOfType[ExtractorGuardianEntity.StartingExtractor].state shouldBe "starting"

      val resultRunning = waitUntilStarted
      resultRunning.stateOfType[ExtractorGuardianEntity.RunningExtractor].state shouldBe "running"

    }

    "update extractor from running state" in {
      eventSourcedTestKit.runCommand[StatusReply[Summary]](ref => ExtractorGuardianEntity.updateExtractor(extData, ref))
      eventSourcedTestKit.runCommand[StatusReply[Done]](ref => ExtractorGuardianEntity.startExtractor(ref))
      val schema = new DataSchema("ayto:idSensor", "dc:modified",
        List(new MeasureField("ocupation","ayto:ocupacion","veh/h",Some("Vehículos por hora sobre una espiga")),
          new MeasureField("intensity", "ayto:intensidad","%")))

      val newRegion = "Cantabric coast"

      val metadataUpdate = Metadata("traffic-santander",Some("Santander traffic flow sensors"),Seq("traffic","static"),
        new Sample(1,"seconds"),
        new Location("santander city",city=Some("Santander"),region=Some(newRegion),country=Some("Spain")),
        url = Some("http://datos.santander.es/dataset/?id=datos-trafico"))

      val resultPost = waitUntilStarted
      resultPost.stateOfType[ExtractorGuardianEntity.RunningExtractor].state shouldBe "running"

      val extData2 = ExtractorState(schema,config, ExtractorType.Http,metadataUpdate)
      eventSourcedTestKit.runCommand[StatusReply[Summary]](ref => ExtractorGuardianEntity.updateExtractor(extData2, ref))

      val result = waitUntilStarted

      result.stateOfType[ExtractorGuardianEntity.RunningExtractor].state shouldBe "running"
      result.stateOfType[ExtractorGuardianEntity.RunningExtractor].extractorState.metadata.localization.region.get shouldBe newRegion
    }

    "work if everything is correct" in {
      eventSourcedTestKit.runCommand[StatusReply[Summary]](ref => ExtractorGuardianEntity.updateExtractor(extData, ref))
      eventSourcedTestKit.runCommand[StatusReply[Done]](ref => ExtractorGuardianEntity.startExtractor(ref))

      val config = system.settings.config.getConfig("akka.kafka.consumer")
      val consumerSettings =
        ConsumerSettings(config, new StringDeserializer, new StringDeserializer)
          .withBootstrapServers(kafkaConfig.server)
          .withGroupId(kafkaConfig.topic)
          .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

      val stream = Consumer.atMostOnceSource(
        consumerSettings,
        Subscriptions.topics(kafkaConfig.topic)
      )
        .map(record => record.value())
        .take(4)
        .runWith(Sink.seq)



      val expectedResult = Set(
        """{"measure":70.0,"city":"Santander","sampling_unit":"seconds","timestamp":"2021-03-23T12:04:00Z","measure_desc":"Vehículos por hora sobre una espiga","measureID":0,"country":"Spain","measure_name":"ocupation","sensorID":"1001","unit":"veh/h","seriesID":"101001","name":"traffic-santander","sourceID":"1","description":"Santander traffic flow sensors","sampling_freq":1,"tags":["traffic","static"],"region":"Santander","address":""}"""
        ,"""{"measure":360.0,"city":"Santander","sampling_unit":"seconds","timestamp":"2021-03-23T12:04:00Z","measure_desc":"","measureID":1,"country":"Spain","measure_name":"intensity","sensorID":"1001","unit":"%","seriesID":"111001","name":"traffic-santander","sourceID":"1","description":"Santander traffic flow sensors","sampling_freq":1,"tags":["traffic","static"],"region":"Santander","address":""}""",
        """{"measure":2.0,"city":"Santander","sampling_unit":"seconds","timestamp":"2021-03-23T12:04:00Z","measure_desc":"Vehículos por hora sobre una espiga","measureID":0,"country":"Spain","measure_name":"ocupation","sensorID":"1002","unit":"veh/h","seriesID":"101002","name":"traffic-santander","sourceID":"1","description":"Santander traffic flow sensors","sampling_freq":1,"tags":["traffic","static"],"region":"Santander","address":""}""",
        """{"measure":120.0,"city":"Santander","sampling_unit":"seconds","timestamp":"2021-03-23T12:04:00Z","measure_desc":"","measureID":1,"country":"Spain","measure_name":"intensity","sensorID":"1002","unit":"%","seriesID":"111002","name":"traffic-santander","sourceID":"1","description":"Santander traffic flow sensors","sampling_freq":1,"tags":["traffic","static"],"region":"Santander","address":""}"""
         )

      val results = Await.result(stream, 5.second).toSet
      results === expectedResult shouldBe true
    }
}

}
