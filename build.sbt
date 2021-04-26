import Dependencies._
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.{dockerChmodType, dockerPermissionStrategy}
import com.typesafe.sbt.packager.docker.{DockerChmodType, DockerPermissionStrategy}
import sbt.Tests.Setup
lazy val buildSettings = Seq(
  organization := "com.iotse",
  version := "0.1.0",
  scalaVersion := "2.13.5",
  libraryDependencies ++= commonDeps,

  dockerChmodType := DockerChmodType.UserGroupWriteExecute,
  dockerPermissionStrategy := DockerPermissionStrategy.CopyChown
)

lazy val iotse = (project in file ("."))
  .aggregate(extractor)
  .settings(buildSettings)

lazy val extractorDockerSettings = Seq(
  Docker / maintainer := "juanheliosg@correo.ugr.es", //
  packageName in Docker := "iot-se-extractor",
  Docker / version := sys.env.getOrElse("BUILD_NUMBER", "0"),
  dockerExposedPorts := Seq(1600),
  dockerBaseImage := "openjdk:8-jre-alpine",
  dockerUpdateLatest := true
)

lazy val extractor = (project in file("extractor"))
  .enablePlugins(PlayScala, AshScriptPlugin, DockerPlugin)
  .settings(
    Test / scalaSource :=  baseDirectory.value / "/test/",
    buildSettings,
    name := "extractor",
    libraryDependencies ++= extractorDeps,
).settings(extractorDockerSettings)


testOptions += Setup( cl =>
  cl.loadClass("org.slf4j.LoggerFactory").
    getMethod("getLogger",cl.loadClass("java.lang.String")).
    invoke(null,"ROOT")
)
 


      
