import requests
import json
import pathlib
import sys
path = pathlib.Path(__file__).parent.absolute()

if len(sys.argv) < 2:
    print("Must provide ingestion spec file")
    sys.exit()

druid = "http://localhost:8081/druid/indexer/v1/supervisor/"
f = open(str(sys.argv[1]))
data = json.load(f)

response = requests.post(druid, json=data)
print(response)
print(response.content)

