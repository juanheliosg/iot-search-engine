import sbt._
import play.sbt.PlayImport.{clusterSharding, guice, ws}

object Dependencies{
   val scalaTestVersion = "3.2.5"

   val logs = "ch.qos.logback" % "logback-classic" % "1.2.3"

   val AkkaVersion = "2.6.13"
   val alpakkaJSON = "com.lightbend.akka" %% "akka-stream-alpakka-json-streaming" % "2.0.2"
   val akka = "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion
   val akkaPersistence = "com.typesafe.akka" %% "akka-persistence-typed" % AkkaVersion
   val akkaSharding = "com.typesafe.akka" %% "akka-cluster-sharding-typed" % AkkaVersion
   val akkaStreams = "com.typesafe.akka" %% "akka-stream-typed" % AkkaVersion
   val akkaSerialization = "com.typesafe.akka" %% "akka-serialization-jackson" % AkkaVersion
   val akkaKafka = "com.typesafe.akka" %% "akka-stream-kafka" % "2.0.7";

   val akkaPersistanceTest = "com.typesafe.akka" %% "akka-persistence-testkit" % AkkaVersion % Test
   val akkaTestKit = "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test

   val sprayJson = "io.spray" %%  "spray-json" % "1.3.6"

   val testContainerVersion = "1.15.2"

   val kafkaClient = "org.apache.kafka" % "kafka-clients" % "2.4.0"
   val kafkaTestContainers = "org.testcontainers" % "kafka" % testContainerVersion
   val cassandraTestContainer = "org.testcontainers" % "cassandra" % testContainerVersion
   val cassandraAkka = "com.typesafe.akka" %% "akka-persistence-cassandra" % "1.0.5"

   val levelDB = "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8"

   val scalastic = "org.scalactic" %% "scalactic" % scalaTestVersion
   val scalaTest = "org.scalatest" %% "scalatest" % scalaTestVersion
   val playScalaTest = "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0"
   val mockitoScala = "org.mockito" %% "mockito-scala" % "1.16.0"

   val slickVersion = "5.0.0"
   val slick =   "com.typesafe.play" %% "play-slick" % slickVersion
   val postgresDriver = "org.postgresql" % "postgresql" % "9.4-1206-jdbc42"


   val commonDeps = Seq(scalaTest % Test, mockitoScala % Test, scalastic, logs, guice)
   val extractorDeps = Seq(playScalaTest % Test, ws,
      akkaPersistanceTest, akkaTestKit,
      akka, akkaPersistence, akkaSharding,clusterSharding, cassandraAkka,
      akkaKafka, akkaStreams, alpakkaJSON, akkaSerialization, sprayJson,
      kafkaClient, kafkaTestContainers % Test, cassandraTestContainer % Test)

   val adminDeps = Seq(playScalaTest % Test, slick, postgresDriver)
}