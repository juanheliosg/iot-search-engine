import numpy as np
from stumpy.core import mass


def get_timedelta(sampling_unit, sampling_freq):
    """
    Function returning a numpy timedelta from params
    :param sampling_unit: time unit
    :param sampling_freq: tiem freq
    :return: numpy timedelta65
    """
    if sampling_unit == "milliseconds":
        return np.timedelta64(sampling_freq/1000,'s')
    elif sampling_unit == "seconds":
        return np.timedelta64(sampling_freq,'s')
    elif sampling_unit == "minute":
        return np.timedelta64(sampling_freq, 'm')
    elif sampling_unit == "hour":
        return np.timedelta64(sampling_freq, 'h')
    elif sampling_unit == "day":
        return np.timedelta64(sampling_freq, 'D')
    elif sampling_unit == "year":
        return np.timedelta64(sampling_freq, 'Y')


def get_series(row, subseq_len):
    """
    This functions  split a time series in continuos intervals of time depending on
    the sampling unit and freq in the row. Time series with values with time differences greater
    than double the sampling period will be split in two different time series
    :param row: dictionary containing a timestamp array, a values array, a series_id str and
    sampling unit int and sampling freq str.
    :param subseq_len: len of the subsequence to be compared
    :return: np array of objects with series_id, offset and values array. Offset determines the beginning
    of the sequence in the original time series
    """
    timestamps = row['timestamp']
    values = row['value']
    series_id = row['series_id']
    ordered_inds = np.argsort(timestamps)
    ordered_timestamps = timestamps[ordered_inds]
    ordered_values = values[ordered_inds]

    sampling_delta = get_timedelta(row['sampling_unit'],row['sampling_freq'])
    timedeltas = ordered_timestamps[1:] - ordered_timestamps[:-1] - sampling_delta
    timedeltas_fix = np.concatenate([[0],timedeltas])
    index_shifts = np.argwhere(abs(timedeltas_fix) > sampling_delta*2).flatten()

    series_list = []
    if (len(index_shifts) > 0 and len(values) >= subseq_len):
        first_values = ordered_values[0:index_shifts[0]]
        if (len(first_values) >= subseq_len):
            series_list.append({
            "series_id": series_id,
            "offset": 0,
            "values": first_values})

        for i in range(1, len(index_shifts)):
            i_values = ordered_values[index_shifts[i - 1]:index_shifts[i]]
            if (len(i_values) >= subseq_len):
                series_list.append({
                "series_id": series_id,
                "offset": i,
                "values": i_values})

        last_values = ordered_values[index_shifts[len(index_shifts) - 1]:]
        if (len(last_values) >= subseq_len):
            series_list.append({
                "series_id": series_id,
                "offset": index_shifts[len(index_shifts) - 1],
                "values": ordered_values[index_shifts[len(index_shifts) - 1]:]})
    elif len(values) >= subseq_len:
        series_list.append({
            "series_id": series_id,
            "offset": 0,
            "values": ordered_values})

    if (len(series_list) > 0):
        return np.array(series_list)
    else:
        return np.array([{
            "series_id": -1,
            "offset": 0,
            "values": [-1]}])

def search_subsequences(subsequence, row, k):
    """
    Search k nearest subsequences of row['values'] to the given subsequence using
    MASS method  from Matrix Profile computation

    :param subsequence: subsequence to be searched
    :param row: row containing values, offset and series_id
    :param k: number of nearest subseq
    :return: a numpy array of objects with series_id: int, ed: float  and start:int values.
    """
    distance_profile = mass(subsequence, row['values'])
    offset = row['offset']  # Partitioned series must have an offset to match with the original one
    if (k < len(distance_profile)):
        idxs = np.argpartition(distance_profile, k)[:k]
        idxs_def = idxs[np.argsort(distance_profile[idxs])]
        subseq = [{"series_id": row['series_id'], "ed": float(distance_profile[i]), "start": int(i + offset)} for i in idxs_def]
        return np.array(subseq)
    else:
        subseq = [{"series_id": row['series_id'], "ed": float(x), "start": int(ind + offset)} for ind, x in
                  enumerate(distance_profile)]
        return np.array(subseq)




