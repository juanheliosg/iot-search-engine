package v1.test.http

import org.scalatest.wordspec.AnyWordSpec
import v1.extractor.http.HTTPExtractor
import v1.extractor.models.extractor.DataSchema
import v1.extractor.models.extractor.{DataSchema, MeasureField}

class HTTPExtractorTest extends AnyWordSpec {


  private val schema = new DataSchema("ayto:idSensor", "dc:modified", List(new MeasureField("ocupation","ayto:ocupacion","veh/h",Some("Vehículos por hora sobre una espiga")),
    new MeasureField("intensity", "ayto:intensidad","%",None)))

  "JSON parse " must {
    "pass nil object if empty measures are given" in {
      val rawSensorData =
        """
          |{
          |      "ayto:ocupacion":"",
          |      "ayto:medida":"1001",
          |      "ayto:idSensor":"1001",
          |      "ayto:intensidad":"",
          |      "dc:modified":"2021-03-23T12:04:00Z",
          |      "dc:identifier":"1001-947db1ed-8bcf-11eb-9073-005056a43242",
          |      "ayto:carga":""
          |      }
          |""".stripMargin

      val result = HTTPExtractor.parseJSON("2",rawSensorData,schema)
      assert(result == Nil)
    }
    "pass just one measure if others empty" in {
      val rawSensorData =
        """
          |{
          |      "ayto:ocupacion":"420",
          |      "ayto:medida":"1001",
          |      "ayto:idSensor":"1001",
          |      "ayto:intensidad":"",
          |      "dc:modified":"2021-03-23T12:04:00Z",
          |      "dc:identifier":"1001-947db1ed-8bcf-11eb-9073-005056a43242",
          |      "ayto:carga":""
          |      }
          |""".stripMargin

      val result = HTTPExtractor.parseJSON("1",rawSensorData,schema)
      println(result(0).toString())

      val expectedResult =
        "{\"measure\":420.0,\"timestamp\":\"2021-03-23T12:04:00Z\",\"measure_desc\":\"Vehículos por hora sobre una espiga\",\"measureID\":0,\"sensorID\":\"1001\",\"unit\":\"veh/h\",\"seriesID\":\"101001\",\"name\":\"ocupation\",\"sourceID\":\"1\"}"
      assert(result(0).toString() == expectedResult)
    }

  }


}
