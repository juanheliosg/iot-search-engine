"""
Script for posting some extractors.
"""
import requests
import json

extractor_url = "http://localhost:1700/v1/extractor"
kafkaTopic = "tseries"
kafkaServer = "kafka:9092" 

santanderCars = {
        "type": "http",
        "dataSchema": {
            "sensorIDField": "ayto:idSensor",
            "timestampField": "dc:modified",
            "measures": [
                {"name": "ocupation",
                "field": "ayto:ocupacion",
                "unit": "veh/h",
                "description": "Número de vehículos contados expandidos a la hora"},
                
                {"name": "intensity",
                "field": "ayto:intensidad",
                "unit": "%",
                "description": "Porcentaje de tiempo de la espira que está ocupada por un vehículo"
                },
                
                {"name": "carga",
                "field": "ayto:carga",
                "unit":"sin unidad",
                "description":"Carga representa una estimación del grado de congestión en base a la intensidad y la ocupación"
                }
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
        },
        "metadata":{
            "name": "santander-traffic",
            "description": "Sensores sobre la intensidad del tráfico en la ciudad de Santander",
            "tags": ["traffic","static","smartcity"],
            "sample":{
                "freq": 1,
                "unit": "minute",
            },
            "localization":{
                "name": "Santander",
                "city": "Santander",
                "region": "Cantabria",
                "country": "Spain"
            }
        }
    }


sensorSmartMobile = {
        "type": "http",
        "dataSchema": {
            "sensorIDField": "dc:identifier",
            "timestampField": "dc:modified",
            "measures": [
                {"name": "NO2",
                "field": "ayto:NO2",
                "unit":"mg/m3",
                },
                {"name": "CO",
                "field": "ayto:CO",
                "unit": "mg/m3"},

                {"name": "Ozone",
                "field": "ayto:ozone",
                "unit": "mg/m3"},

                 {"name": "Temperature",
                "field": "ayto:temperature",
                "unit":"celsius"},
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
        },
        "metadata":{
            "name": "santander-environment",
            "description": "Sensores ambientales móviles en la ciudad de Santander",
            "tags": ["environment","mobile","smartcity","air quality"],
            "sample":{
                "freq": 1,
                "unit": "minute",
            },
            "localization":{
                "name": "Santander",
                "city": "Santander",
                "region": "Cantabria",
                "country": "Spain"
            }
        }
    }
sensorSmartEnvMonitoring = {
        "type": "http",
        "dataSchema": {
            "sensorIDField": "dc:identifier",
            "timestampField": "dc:modified",
            "measures": [
                {"name": "Noise",
                "field": "ayto:noise",
                "unit":"dB",
                },
                {"name": "Light",
                "field": "ayto:light",
                "unit": "lm"},

                {"name": "Temperature",
                "field": "ayto:temperature",
                "unit": "celsius"}
                ]
            },
        "IOConfig":{
            "inputConfig": {
                "address": "http://datos.santander.es/api/rest/datasets/sensores_smart_env_monitoring.json",
                "jsonPath": "$.resources",
                "freq": 60000 #1 Minute
            },
            "kafkaConfig": {
                "topic": kafkaTopic,
                "server": kafkaServer
            }
        },
        "metadata":{
            "name": "santander-environment-",
            "description": "Mediciones en tiempo real de distintos sensores localizados en la ciudad de Santander relacionados con el ambiente, luz, ruido, temperatura...",
            "tags": ["environment","mobile","smartcity","noise","light"],
            "sample":{
                "freq": 1,
                "unit": "minute",
            },
            "localization":{
                "name": "Santander",
                "city": "Santander",
                "region": "Cantabria",
                "country": "Spain"
            }
        }
    }

sensorContainers = {
        "type": "http",
        "dataSchema": {
            "sensorIDField": "ayto:codigoSensor",
            "timestampField": "dc:modified",
            "measures": [
                {"name": "Fill level",
                "field": "ayto:nivelLlenado",
                "unit":"%",
                "description":"Nivel de llenado del contenedor expresado en porcentaje"
                },

                {"name": "Temperature",
                "field": "ayto:temperatura",
                "unit": "celsius"}
                ]
            },
        "IOConfig":{
            "inputConfig": {
                "address": "http://datos.santander.es/rest/datasets/residuos_contenedores.json",
                "jsonPath": "$.resources",
                "freq": 60000 #1 Minute
            },
            "kafkaConfig": {
                "topic": kafkaTopic,
                "server": kafkaServer
            }
        },
        "metadata":{
            "name": "santander-waste-management",
            "description": "Este recurso proporciona información sobre el estado de los contenedores de residuos",
            "tags": ["waste","static","smartcity","containers"],
            "sample":{
                "freq": 1,
                "unit": "minute",
            },
            "localization":{
                "name": "Santander",
                "city": "Santander",
                "region": "Cantabria",
                "country": "Spain"
            }
        }
    }

sensorNoise = {
        "type": "http",
        "dataSchema": {
            "sensorIDField": "name",
            "timestampField": "modified",
            "measures": [
                {"name": "Noise",
                "field": "LAeq",
                "unit":"unknow",
                "description":"Nivel sonoro continuo equivalente. Se define en la ISO 1996-2:2017 como el valor del nivel de presión en dBA en ponderacion con el tiempo."
                }]
            },
        "IOConfig":{
            "inputConfig": {
                "address": "https://apigobiernoabiertortod.valencia.es/rest/datasets/estado_sonometros_cb.json",
                "jsonPath": "$.resources",
                "freq": 60000 #1 Minute
            },
            "kafkaConfig": {
                "topic": kafkaTopic,
                "server": kafkaServer
            }
        },
        "metadata":{
            "name": "valencia-ruzafa-noise",
            "description": "Sensores de ruido del barrio de Ruzafa de valencia",
            "tags": ["static","smartcity","noise"],
            "sample":{
                "freq": 1,
                "unit": "minute",
            },
            "localization":{
                "name": "Barrio de Ruzafa",
                "city": "Valencia",
                "region": "Valencia",
                "country": "Spain"
            }
        }
    }


sensors = [sensorSmartMobile,santanderCars,sensorSmartEnvMonitoring, sensorNoise, sensorContainers]

for ext in sensors:
    response = requests.post(extractor_url, json=ext)   
    print("Status code: ", response)
    print(response.content)






