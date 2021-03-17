import Dependencies._

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
    buildSettings,
    name:= "extractor",
    libraryDependencies ++= extractorDeps
  )
 


      