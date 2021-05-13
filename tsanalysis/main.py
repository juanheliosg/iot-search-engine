from fastapi import FastAPI
from numpy import array, vectorize, concatenate
from pandas import Series

from utils.models import SeriesQuery
from utils.subseq import get_series, search_subsequences

app = FastAPI()


@app.post("/subsequence/search")
async def subsequence_search(query: SeriesQuery):
    """
    Search for a subsequence in several time series
    :param query: object containing the subsequence, time series and k_nearest info
    :return: a list of {series_id: str, ed: float, start: int} representing a subsequence result
    """
    ts = Series(query.time_series).apply(lambda x: x.as_np())
    np_raw = array(ts)
    subsequence = array(query.subsequence)
    v_get_series = vectorize(lambda row: get_series(row, len(subsequence)))
    v_get_non_empty = vectorize(lambda row: row['series_id'] != -1)
    v_mass_series = vectorize(lambda row: search_subsequences(subsequence, row, query.k_nearest - 1))

    np_divided_series = concatenate(v_get_series(np_raw))
    np_non_empty_series = np_divided_series[v_get_non_empty(np_divided_series)]
    result_len = len(np_non_empty_series)

    if result_len > 0:
        results = list(concatenate(v_mass_series(np_non_empty_series)))
        return {"items": result_len, "results": results}

    else:
        return {"items": "0", "results": []}
