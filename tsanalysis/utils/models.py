from enum import Enum
from typing import List

import numpy as np
from pydantic import BaseModel


class SamplingUnits(str, Enum):
    """
    Enum representing different units of time
    """
    milliseconds = "milliseconds"
    seconds = "seconds"
    minutes = "minute"
    hour = "hour"
    day = "day"
    year = "year"


class Series(BaseModel):
    """
    A class representing a series
    Attributes
    ------------
    series_id: str
        a unique identifier for the serie to be analysed
    sampling_freq: int
        sampling frequency of the time series
    sampling_unit: str
        sampling unit can be seconds, minutes, hours, days or year
    timestamps: list(str)
        list of string iso timestamps
    values: list(float)
        list of values in float

    Methods
    --------
    as_np:
        returns a dictionary with numpy arrays for timestamps and values
    """
    series_id: str
    sampling_freq: int
    sampling_unit: SamplingUnits
    timestamps: List[str]
    values: List[float]

    def as_np(self):
        return {"series_id": self.series_id, "sampling_freq": self.sampling_freq,
                "sampling_unit": self.sampling_unit.value,
                "timestamp": np.array(self.timestamps, dtype='datetime64'), "value": np.array(self.values)}


class SeriesQuery(BaseModel):
    """
    A class representing a query

    Attributes
    ----------
    k_nearest: int
        specifies the number of subsequences to retrieve from each series
    subsequence: list(float)
        subsequence to be found in the time series array
    time_series: list(Series)
        list of time series to query
    """
    k_nearest: int
    subsequence: List[float]
    time_series: List[Series]
