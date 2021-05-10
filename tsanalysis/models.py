from pydantic import BaseModel
from datetime import datetime
from typing import List, Optional
from enum import Enum


class SamplingUnits(str, Enum):
    milliseconds = "milliseconds"
    seconds = "seconds"
    minutes = "minutes"
    hour = "hour"
    day = "day"
    month = "month"
    year = "year"


class Subsequence(BaseModel):
    equality: Optional[bool] = True
    normalization: Optional[bool] = True
    series: List[float]

class Measure(BaseModel):
    timestamp: datetime
    value: float


class Series(BaseModel):
    series_id: str
    sampling_freq: int
    sampling_unit: SamplingUnits
    series: List[Measure]



class SeriesQuery(BaseModel):
    k_nearest: int
    subsequence: Subsequence
    time_series: List[Series]


class SubsequenceResponse:
    ed: float
    start: int
    end: int


class SeriesQueryResponse:
    series_id: str
    subsequences: List[SubsequenceResponse]
