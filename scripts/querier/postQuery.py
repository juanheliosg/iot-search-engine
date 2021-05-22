"""
Script for posting some extractors.
"""
import requests
import json
import datetime

querier_url = "http://localhost:9000/v1/query"

santander_traffic_query = {
    "limit": 100,
    "timeRange": [{
        "lowerBound": (datetime.datetime.utcnow() - datetime.timedelta(hours=1)).isoformat(),
        "upperBound": datetime.datetime.utcnow().isoformat()}],
    "type": "aggregation",
    "filter": "tags = 'traffic' AND measure_name = 'ocupation'",
    "aggregationFilter" : [{
        "operation" : "avg",
        "aggComparation": "avg",
        "relation": ">"
    }]
}
santander_traffic_query_subseq = {
    "limit": 100,
    "timeRange": [{
        "lowerBound": (datetime.datetime.utcnow() - datetime.timedelta(hours=1)).isoformat(),
        "upperBound": datetime.datetime.utcnow().isoformat()}],
    "type": "complex",
    "timeseries": True,
    "filter": "tags = 'traffic' AND measure_name = 'ocupation'",
    "aggregationFilter" : [{
        "operation" : "avg",
        "aggComparation": "avg",
        "relation": ">"
    }],
    "subsequenceQuery" : {
        "subsequence": [0,1,1,0]
    }
}

santander_traffic_query_with_series = {
    "limit": 100,
    "timeRange": [{
        "lowerBound": (datetime.datetime.utcnow() - datetime.timedelta(hours=1)).isoformat(),
        "upperBound": datetime.datetime.utcnow().isoformat()}], 
    "type": "aggregation",
    "timeseries": True,
    "filter": "tags = 'traffic' AND measure_name = 'ocupation'",
    "subsequenceQuery": {
            "subsequence": [0,1,1,0]
        },
    "aggregationFilter" : [{
        "operation" : "avg",
        "aggComparation": "avg",
        "relation": ">"
    }]
}
queries = [santander_traffic_query, santander_traffic_query_subseq]

for q in queries:
    response = requests.post(querier_url, json=q)
    print("Status code: ", response)
    print(response.content)
    print("\n \n")
    assert(response.status_code == 200)
