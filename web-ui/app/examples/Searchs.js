
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
                    "lowerBound": currentDate,
                    "upperBound": lastDayDate
                }
            ],
            "type" : "simple",
            "filter" : "tags LIKE 'traffic'"
        },
        {
            "limit" : 100,
            "timeseries" : true,
            "timeRange" : [
                {
                    "lowerBound": currentDate,
                    "upperBound": lastDayDate
                }
            ],
            "type" : "aggregation",
            "filter" : "tags LIKE 'traffic' AND measure_name = 'ocupation'",
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
                    "lowerBound": currentDate,
                    "upperBound": lastDayDate
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