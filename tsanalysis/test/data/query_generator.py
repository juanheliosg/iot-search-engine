import datetime
import numpy as np
import json


def generate_broken_series_arrays(min_size: int, max_size: int, init_timestamp: datetime.datetime):
    possible_sizes = np.arange(min_size, max_size)
    size = np.random.choice(possible_sizes)
    timestamps = np.array([init_timestamp + datetime.timedelta(minutes=j)
                           if j > size / 2 else init_timestamp + datetime.timedelta(minutes=j) + datetime.timedelta(
        days=1)
                           for j in range(0, size)])
    values = np.random.rand(size)

    return([x.isoformat() for x in timestamps],list(values))


def generate_query_fast_fmt(nseries: int, min_size: int, max_size: int, size_subseq: int, init_timestamp: datetime.datetime, series_generator):
    time_series = []
    for i in range(0,nseries):
        ts = generate_broken_series_arrays(min_size, max_size, init_timestamp)
        time_series.append(
            {"series_id": i, "sampling_freq": 1, "sampling_unit": "minutes",
             "timestamps": ts[0], "values": ts[1]}
        )

    subseq = np.random.rand(size_subseq)

    query = {
        "k_nearest": 10,
        "subsequence": list(subseq),
        "time_series": time_series
    }
    return query



#with open('./broken_query_subseq_big_fast.json', 'w') as json_file:
   #json.dump(generate_query_fast_fmt(2000, 3600, 3601, 20, datetime.datetime.now(), generate_broken_series_arrays),json_file,indent=4,sort_keys=True)

#with open('./short_query_subseq_fast.json', 'w') as json_file:
    #json.dump(generate_query_fast_fmt(10, 10, 11, 20, datetime.datetime.now(), generate_broken_series),json_file,indent=4,sort_keys=True)

##with open('./broken_query_subseq_medium_fast.json', 'w') as json_file:
  ##  json.dump(generate_query_fast_fmt(100, 3600, 3601, 20, datetime.datetime.now(), generate_broken_series),json_file,indent=4,sort_keys=True)
