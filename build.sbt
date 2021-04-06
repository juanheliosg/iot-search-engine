import Dependencies._
import sbt.Tests.Setup
lazy val buildSettings = Seq(
  organization := "com.iotse",
  version := "0.1.0",
  scalaVersion := "2.13.5",
  libraryDependencies ++= commonDeps
)

lazy val iotse = (project in file ("."))
  .aggregate(extractor)
  .settings(buildSettings)

lazy val extractor = (project in file("extractor"))
  .enablePlugins(PlayScala)
  .settings(
    Test / scalaSource :=  baseDirectory.value / "/test/",
    buildSettings,
    name := "extractor",
    libraryDependencies ++= extractorDeps
  )

testOptions += Setup( cl =>
  cl.loadClass("org.slf4j.LoggerFactory").
    getMethod("getLogger",cl.loadClass("java.lang.String")).
    invoke(null,"ROOT")
)
 


      