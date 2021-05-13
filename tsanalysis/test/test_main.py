from fastapi.testclient import TestClient
import json

from main import app

client = TestClient(app)

def test_post_0_items_subseq():
    with open("./test/data/short_query_subseq_fast.json", 'r') as jsonfile:
        jsonQuery = json.dumps(json.load(jsonfile))
        response = client.post("/subsequence/search",jsonQuery).json()
        assert(response['items'] == "0")


def test_post_subseq():
    with open("./test/data/query_subseq_fast.json", 'r') as jsonfile:
        jsonQuery = json.dumps(json.load(jsonfile))

        response = client.post("/subsequence/search",jsonQuery).json()
        print(response)
        assert(response['items'] > 0)
        assert("ed" in response['results'][0])
        assert ("series_id" in response['results'][0])
        assert ("start" in response['results'][0])
