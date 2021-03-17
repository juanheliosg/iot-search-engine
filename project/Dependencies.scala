import sbt._
import play.sbt.PlayImport.guice

object Dependencies{
   val scalaTestVersion = "3.2.5"

   val logs = "ch.qos.logback" % "logback-classic" % "1.2.3"

   val scalastic = "org.scalactic" %% "scalactic" % scalaTestVersion
   val scalaTest = "org.scalatest" %% "scalatest" % scalaTestVersion % "test"

   val commonDeps = Seq(scalaTest, scalastic, logs, guice)
   val extractorDeps = Seq()
}