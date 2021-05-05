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

      val expectedResult =
        "{\"measure\":420.0,\"sourceID\":\"1\",\"timestamp\":\"2021-03-23T12:04:00Z\",\"measure_desc\":\"Vehículos por hora sobre una espiga\",\"measureID\":0,\"measure_name\":\"ocupation\",\"long\":\"\",\"sensorID\":\"1001\",\"unit\":\"veh/h\",\"seriesID\":\"101001\",\"lat\":\"\"}"
      assert(result(0).toString() == expectedResult)
      }
    }
  "JSON recursive parse" must {
    "work correctly for nested structures" in {
      val rawSensorData =
      """
      {
        "type": "Feature",
        "id": "07012",
        "geometry": {
          "type": "Point",
          "coordinates": [
          -2.69808837,
          42.83743369
          ]
        },
        "properties": {
          "nombre": "07012",
          "startDate": "2021-05-05T12:15:00",
          "volume": 101,
          "load": 37.667,
          "endDate": "2021-05-05T12:30:00",
          "tipo_inventario": "MS",
          "proveedor": "OPTIMUS",
          "occupancy": "",
          "type": "MS"
        }
      }"""

      val nestedSchema = new DataSchema("id", "endDate",
        List(new MeasureField("ocupation","occupancy","veh/h",Some("Vehículos por hora sobre una espiga")),
        new MeasureField("load", "load","%",None)))
      val result = HTTPExtractor.parseRecursiveJSON("1",rawSensorData,nestedSchema)
      println(result)
      val expectedResult = "{\"measure\":37.667,\"sourceID\":\"1\",\"timestamp\":\"2021-05-05T12:30:00\",\"measure_desc\":\"\",\"measureID\":1,\"measure_name\":\"load\",\"long\":\"\",\"sensorID\":\"07012\",\"unit\":\"%\",\"seriesID\":\"1107012\",\"lat\":\"\"}"
      assert(result(0).toString() == expectedResult)

    }
  }

  }

