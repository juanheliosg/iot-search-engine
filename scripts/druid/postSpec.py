import requests
import json
import pathlib
path = pathlib.Path(__file__).parent.absolute()

druid = "http://localhost:8081/druid/indexer/v1/supervisor/"
f = open("{}/supervisor-spec.json".format(path))
data = json.load(f)

response = requests.post(druid, json=data)
print(response)
print(response.content)

