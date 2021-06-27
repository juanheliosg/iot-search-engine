from locust import HttpUser, task, between, events
from contextlib import contextmanager, ContextDecorator
from time import time, sleep
import random
import config
import queries

csv_rows = []

@events.request.add_listener
def custom_success_handler(request_type, name, response_time, response_length, response,
                       context, exception, **kwargs):
    """ additional request success handler to log statistics """

    print(csv_rows)
    csv_rows.append([request_type,name,context['name'],context['query_ind'],
                                               response_time,response_length, response.status_code])


@events.test_stop.add_listener
def quitting(environment, **kwargs):
    import csv
    print(environment)
    with open('testresults.csv', 'w') as csv_file:
        writer = csv.writer(csv_file)
        writer.writerows(csv_rows)






class QuerierUser(HttpUser):
    wait_time = between(config.min_wait_time, config.max_wait_time)

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.simple_queries_ind = random.randint(0, len(queries.simple_queries))
        self.agg_queries_without_ts_ind = random.randint(0, len(queries.agg_queries_without_series))
        self.agg_queries_with_ts_ind = random.randint(0, len(queries.agg_queries_with_ts))
        self.complex_queries_ind = random.randint(0, len(queries.complex_queries))
        self.utility_ind = random.randint(0, len(config.utility_endpoints))

    @task(8)
    def utility_queries(self):
        ind = self.utility_ind % len(config.utility_endpoints)
        self.client.get(config.utility_endpoints[ind],
                        context={"name":"utility_query", "query_ind": ind})
        self.utility_ind += 1

    @task(8)
    def simple_queries(self):
        ind = self.simple_queries_ind % len(queries.simple_queries)
        self.client.post(config.query_endpoint, json=
        queries.simple_queries[ind],
                         context={"name":"simple_query", "query_ind": ind})

        self.simple_queries_ind += 1

    @task(4)
    def agg_queries_without_series(self):
        ind = self.agg_queries_without_ts_ind % len(queries.agg_queries_without_series)
        self.client.post(config.query_endpoint, json=
        queries.agg_queries_without_series[ind],
                         context={"name":"agg_without_ts_query", "query_ind": ind})
        self.agg_queries_without_ts_ind += 1

    @task(4)
    def agg_queries_with_series(self):
        ind = self.agg_queries_with_ts_ind % len(queries.agg_queries_with_ts)
        self.client.post(config.query_endpoint, json=
        queries.agg_queries_with_ts[ind],
                         context={"name":"agg_with_ts_query", "query_ind": ind})
        self.agg_queries_with_ts_ind += 1

    @task(2)
    def complex_queries(self):
        ind = self.complex_queries_ind % len(queries.complex_queries)
        self.client.post(config.query_endpoint, json=
        queries.complex_queries[ind],
                         context={"name":"complex_query", "query_ind": ind})
        self.complex_queries_ind += 1




