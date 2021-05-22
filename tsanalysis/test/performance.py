from fastapi.testclient import TestClient
import json
import time

from main import app

client = TestClient(app)

with open("./data/broken_query_subseq_big_fast.json", 'r') as jsonfile:
    jsonQuery = json.dumps(json.load(jsonfile))
    start = time.time()
    response = client.post("/subsequence/search", jsonQuery).json()
    end = time.time()
    print(end - start)