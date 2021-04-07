"""
Script for posting some extractors.
"""
import requests
import json

extractor_url = "http://localhost:1700/v1/extractor"
kafkaTopic = "tseries"
kafkaServer = "kafka:9092" 

santanderCars = {
        "id": 1,
        "type": "http",
        "dataSchema": {
            "sourceID": 1,
            "sensorIDField": "ayto:idSensor",
            "timestampField": "dc:modified",
            "measures": [
                {"name": "ocupation",
                "field": "ayto:ocupacion",
                "measureID": 1},
                
                {"name": "intensity",
                "field": "ayto:intensidad",
                "measureID": 2},
                
                {"name": "carga",
                "field": "ayto:carga",
                "measureID": 3}
                ]
            },
        "IOConfig":{
            "inputConfig": {
                "address": "http://datos.santander.es/api/rest/datasets/mediciones.json?",
                "jsonPath": "$.resources",
                "freq": 60000 #1 Minute
            },
            "kafkaConfig": {
                "topic": kafkaTopic,
                "server": kafkaServer 
            }
        }
    }


sensorSmartMobile = {
        "id": 2,
        "type": "http",
        "dataSchema": {
            "sourceID": 2,
            "sensorIDField": "dc:identifier",
            "timestampField": "dc:modified",
            "measures": [
                {"name": "NO2",
                "field": "ayto:NO2",
                "measureID": 4},
                
                {"name": "CO",
                "field": "ayto:CO",
                "measureID": 5},
                
                {"name": "Ozone",
                "field": "ayto:ozone",
                "measureID": 6},

                 {"name": "Temperature",
                "field": "ayto:temperature",
                "measureID": 7},

                 {"name": "Latitude",
                "field": "ayto:latitude",
                "measureID": 8},

                {"name": "Longitude",
                "field": "ayto:longitude",
                "measureID": 9},

                ]
            },
        "IOConfig":{
            "inputConfig": {
                "address": "http://datos.santander.es/api/rest/datasets/sensores_smart_mobile.json",
                "jsonPath": "$.resources",
                "freq": 60000 #1 Minute
            },
            "kafkaConfig": {
                "topic": kafkaTopic,
                "server": kafkaServer 
            }
        }
    }

sensors = [sensorSmartMobile,santanderCars]
for ext in sensors:
    print("Status code: ", requests.post(extractor_url, json=ext))






