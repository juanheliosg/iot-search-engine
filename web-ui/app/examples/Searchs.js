
import sub from 'date-fns/sub'
export default function (){
    let currentDate = new Date()
    let lastDayDate = sub(currentDate,{days: 1})
    return [
        {
            "limit" : 100,
            "timeseries" : false,
            "timeRange" : [
                {
                    "lowerBound": lastDayDate,
                    "upperBound": currentDate
                }
            ],
            "type" : "simple",
            "filter" : "tags IN ('traffic')"
        },
        {
            "limit" : 100,
            "timeseries" : true,
            "timeRange" : [
                {
                    "lowerBound": lastDayDate,
                    "upperBound": currentDate
                }
            ],
            "type" : "aggregation",
            "filter" : "tags IN ('traffic') AND measure_name = 'ocupation'",
            "aggregationFilter" : [
                {
                    "operation": "avg",
                    "value" : 25,
                    "relation" : ">="
                }
            ]
        },
        {
            "limit" : 100,
            "timeseries" : true,
            "timeRange" : [
                {
                    "lowerBound": lastDayDate,
                    "upperBound": currentDate
                },
                {
                    "lowerBound": sub(currentDate,{days: 3}),
                    "upperBound": sub(currentDate,{days: 2})
                }
            ],
            "type" : "complex",
            "aggregationFilter" : [
                {
                    "operation" : "avg",
                    "aggComparation" :"avg",
                    "relation" : ">="
                }
            ],
            "filter" : "measure_name = 'noise'",
            "subsequenceQuery" : {
                "subsequence" : [0,0,1,1,1,1,0,0]
            }
        }
    ]
}