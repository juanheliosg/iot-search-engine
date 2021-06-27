sensoresSantander = {
    "limit": 100,
    "timeRange": [
        {
            "lowerBound": "2021-06-01T16:28:26.000Z",
            "upperBound": "2021-06-26T16:28:26.629Z"
        }
    ],
    "timeseries": False,
    "type": "simple",
    "filter": "city = 'Santander'"
}
sensoresValencia = {
    "limit": 100,
    "timeRange": [
        {
            "lowerBound": "2021-06-01T16:28:26.000Z",
            "upperBound": "2021-06-26T16:28:26.629Z"
        }
    ],
    "timeseries": False,
    "type": "simple",
    "filter": "city = 'Valencia'"
}

sensoresOcupacionSantanderVitoria = {
    "limit": 100,
    "timeRange": [
        {
            "lowerBound": "2021-06-01T16:28:26.000Z",
            "upperBound": "2021-06-26T16:28:26.629Z"
        }
    ],
    "timeseries": False,
    "type": "simple",
    "filter": "(city = 'Santander' OR city = 'Vitoria') AND measure_name='ocupation' "
}

sensoresOzono = {
    "limit": 100,
    "timeRange": [
        {
            "lowerBound": "2021-06-01T16:28:26.000Z",
            "upperBound": "2021-06-26T16:28:26.629Z"
        }
    ],
    "timeseries": False,
    "type": "simple",
    "filter": " tags IN ('environment') AND sampling_unit = 'minute' AND sampling_freq = 1 "
              "AND (measure_name='CO' OR measure_name='Ozone')"
}

noiseLevelMeasure = {
    "limit": 100,
    "timeRange": [
        {
            "lowerBound": "2021-06-11T20:00:00.000Z",
            "upperBound": "2021-06-12T06:00:00.000Z"
        }
    ],
    "timeseries": False,
    "type": "aggregation",
    "filter": """measure_name = 'Noise'""",
    "aggregationFilter": [
        {
            "operation": "avg",

        },
        {
            "operation": "avg",

        },
        {
            "operation": "avg",

            "value": "60",
            "relation": ">="
        }
    ]
}

maxTrafficSensor = {
    "limit": 100,
    "timeseries": False,
    "timeRange": [
        {
            "lowerBound": "2021-06-25T08:00:00.000Z",
            "upperBound": "2021-06-25T15:00:00.000Z"
        }
    ],
    "type": "aggregation",
    "filter": """name = 'santander-traffic' AND unit='veh/h'""",
    "aggregationFilter": [
        {
            "operation": "avg",

            "value": "40",
            "relation": ">="
        }
    ]
}

maxTrafficSensorConSerie = {
    "limit": 100,
    "timeseries": True,
    "timeRange": [
        {
            "lowerBound": "2021-06-25T08:00:00.000Z",
            "upperBound": "2021-06-25T15:00:00.000Z"
        }
    ],
    "type": "aggregation",
    "filter": """name = 'santander-traffic' AND unit='veh/h'""",
    "aggregationFilter": [
        {
            "operation": "avg",

            "value": "40",
            "relation": ">="
        }
    ]
}

porcentajeTraficoSinErrores = {
    "limit": 100,
    "timeseries": False,
    "timeRange": [
        {
            "lowerBound": "2021-06-25T08:00:00.000Z",
            "upperBound": "2021-06-25T15:00:00.000Z"
        }
    ],
    "type": "aggregation",
    "filter": "name = 'santander-traffic' AND unit='%'",
    "aggregationFilter": [
        {
            "operation": "avg",
            "aggComparation": "avg",

            "relation": "<="
        },
        {
            "operation": "avg",

            "value": "100",
            "relation": "<"
        }
    ]
}

traficoSubsecuenciaPlanicie = {
    "limit": 100,
    "timeseries": True,
    "timeRange": [
        {
            "lowerBound": "2021-06-25T08:00:00.000Z",
            "upperBound": "2021-06-25T15:00:00.000Z"
        }
    ],
    "type": "complex",
    "filter": "name = 'santander-traffic' AND unit='%'",
    "aggregationFilter": [
        {
            "operation": "avg",
            "aggComparation": "avg",

            "relation": "<="
        },
        {
            "operation": "avg",

            "value": "100",
            "relation": "<"
        }
    ],
    "subsequenceQuery": {
        "subsequence": [
            0.12381983675324582,
            0.364246744848146,
            0.6427901139824816,
            0.7776637453527915,
            0.868556844754522,
            0.868556844754522,
            0.8451005610379464,
            0.8451005610379464,
            0.8832170220773818,
            0.868556844754522,
            0.871488880219094,
            0.871488880219094,
            0.7395472843133561,
            0.6193338302659059,
            0.37597488670643386,
            0.14434408500524953
        ]
    }
}

sensoresAireMAyoresMedia = {
    "limit": 100,
    "timeseries": False,
    "timeRange": [
        {
            "lowerBound": "2021-06-17T16:15:51.000Z",
            "upperBound": "2021-06-26T16:15:51.204Z"
        }
    ],
    "type": "aggregation",
    "filter": " tags IN ('air quality') AND unit = 'mg/m3' AND measure_name='NO2'",
    "aggregationFilter": [
        {
            "operation": "avg",
            "aggComparation": "avg",

            "relation": ">"
        },
        {
            "operation": "stddev",

        }
    ]
}

valleEnTemperaturaSantander = {
    "limit": 100,
    "timeseries": True,
    "timeRange": [
        {
            "lowerBound": "2021-06-01T16:28:26.000Z",
            "upperBound": "2021-06-26T16:28:26.629Z"
        }
    ],
    "type": "complex",
    "filter": "city = 'Santander' AND unit = 'celsius' AND name='santander-environment'",
    "subsequenceQuery": {
        "subsequence": [
            1,
            0.806984099998511,
            0.6897026814156328,
            0.24110125533612403,
            0.041722843745231164,
            0.009470453634939702,
            0.0036063827057957187,
            0.044654879209803156,
            0.18246054604468498,
            0.40236320588758145,
            0.6046736529430462,
            0.7278191424550683,
            0.8392364901088025,
            0.9271975540459612
        ]
    }
}

contenedoresLlenoTiempoReals = {
    "limit": 100,
    "timeseries": True,
    "timeRange": [
        {
            "lowerBound": "2021-06-25T16:49:00.000Z",
            "upperBound": "2021-06-25T16:53:00.000Z"
        }
    ],
    "type": "aggregation",
    "filter": " tags IN ('waste') AND unit='%'",
    "aggregationFilter": [
        {
            "operation": "avg",

            "value": "90",
            "relation": ">="
        }
    ]
}

ultimaHoraRuidoValencia = {
    "limit": 100,
    "timeseries": True,
    "timeRange": [
        {
            "lowerBound": "2021-06-01T16:00:00.000Z",
            "upperBound": "2021-06-26T16:53:00.000Z"
        }
    ],
    "type": "complex",
    "filter": " name = 'valencia-ruzafa-noise'",
    "aggregationFilter": [],
    "subsequenceQuery": {
        "subsequence": [
            0.37890692217100586,
            0.37890692217100586,
            0.37890692217100586,
            0.28214975184013136,
            0.11209169489495807,
            0.05931505653266289,
            0.0329267373515153,
            0.018266560028655565,
            0.009470453634939702,
            0.06811116292637875,
            0.20884886522583246,
            0.31147010648585083,
            0.37890692217100586,
            0.37890692217100586,
            0.37890692217100586,
            0.37890692217100586
        ]
    }
}

consultaVitoria = {
    "limit": 100,
    "timeseries": False,
    "timeRange": [
        {
            "lowerBound": "2021-06-26T15:05:00.000Z",
            "upperBound": "2021-06-26T17:05:13.207Z"
        }
    ],
    "type": "aggregation",
    "filter": "name = 'vitoria-trafico' AND measure_name='ocupation'",
    "aggregationFilter": [
        {
            "operation": "avg",

        },
        {
            "operation": "max",

        },
        {
            "operation": "min",

        },
        {
            "operation": "avg",
            "aggComparation": "avg",

            "relation": ">="
        }
    ]
}

consultaVitoriaSubseq = {
    "limit": 100,
    "timeseries": True,
    "timeRange": [
        {
            "lowerBound": "2021-06-26T15:05:00.000Z",
            "upperBound": "2021-06-26T17:05:13.207Z"
        }
    ],
    "type": "complex",
    "filter": "name = 'vitoria-trafico' AND measure_name='ocupation'",
    "aggregationFilter": [
        {
            "operation": "avg",

        },
        {
            "operation": "max",

        },
        {
            "operation": "min",

        },
        {
            "operation": "avg",
            "aggComparation": "avg",

            "relation": ">="
        }
    ],
    "subsequenceQuery": {
        "subsequence": [
            0.1502081559343934,
            0.0798393047846665,
            0.9653140150853965,
            1,
            0.22644107801326419,
            0.1531401913989654
        ]
    }
}

consultaNO2PorEncimaNivel = {
    "limit": 100,
    "timeseries": False,
    "timeRange": [
        {
            "lowerBound": "2021-06-01T17:32:45.000Z",
            "upperBound": "2021-06-26T17:32:45.013Z"
        }
    ],
    "type": "aggregation",
    "filter": "measure_name = 'NO2'",
    "aggregationFilter": [
        {
            "operation": "max",

            "value": "200",
            "relation": ">="
        }
    ]
}

simple_queries = [
    sensoresSantander,
    sensoresOcupacionSantanderVitoria,
    sensoresValencia,
    sensoresOzono]

agg_queries_without_series = [
    noiseLevelMeasure,
    maxTrafficSensor,
    porcentajeTraficoSinErrores,
    consultaNO2PorEncimaNivel,
    consultaVitoria,
    sensoresAireMAyoresMedia]


def transform_dict(query):
    q2 = query.copy()
    q2['timeseries'] = True
    return q2


agg_queries_with_ts = [transform_dict(query) for query in agg_queries_without_series]

complex_queries = [
    consultaVitoriaSubseq,
    traficoSubsecuenciaPlanicie,
    ultimaHoraRuidoValencia,
    valleEnTemperaturaSantander]


