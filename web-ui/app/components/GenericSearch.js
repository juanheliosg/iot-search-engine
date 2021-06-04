import { useState } from "react"
import { Form, Button } from "react-bootstrap"
import sub from 'date-fns/sub'
import AdvancedSearch from "./AdvancedSearch"
import SimpleSearch from "./SimpleSearch"

const initialQuery = {
    "limit": 100,
    "timeseries": false,
    "timeRange": [
        {
            "lowerBound": sub(new Date(),{days: 1}),
            "upperBound": new Date()
        }
    ],
    "type": "simple",
    "filter": ""
}

const initialFilter = {
    "tags": new Set(),
    "city": null,
    "region": null,
    "country": null,
    "address": null,
    "measure_name": null,
    "sampling_unit": null,
    "sampling_freq": null
}

/**
 * Component wrapper for a simple and advanced search.
 * This component holds the state of the query
 * 
 * @returns 
 */
const GenericSearch = ({fieldHelp}) => {
    const [simpleSearch, setSimpleSearch] = useState(true)
    const [searchQuery, setSearch] = useState(initialQuery)
    const [simplifiedFilter, setSimpFilter] = useState(initialFilter)


    const simpToNormalSearch = (simplifiedSearch) => {
        let sqlFilter = ""
        console.log(simplifiedSearch)
        for (const key in simplifiedSearch){
            if (simplifiedSearch[key] ){
                if (sqlFilter !== ""){
                    sqlFilter = sqlFilter.concat( " AND ")
                }
                if (key == 'tags')
                {   if (simplifiedSearch.tags > 0){
                    const tags = [...simplifiedSearch[key]]
                    console.log(tags)
                    sqlFilter = sqlFilter.concat(` tags IN ('${tags.join("','")}')`)
                    }
                }
            
                else if (simplifiedSearch[key] !== null && key === 'sampling_freq'){
                    sqlFilter = sqlFilter.concat(`${key} = ${parseInt(simplifiedSearch[key])}`)
                }
                else if (simplifiedSearch[key] !== "none"){
                    sqlFilter = sqlFilter.concat( `${key} = '${simplifiedSearch[key]}'`)
                }
            }
        }
        setSearch({
            ...searchQuery, filter: sqlFilter
        })

    } 

    return(
        <>
        {simpleSearch?<SimpleSearch
                        fieldHelp={fieldHelp}
                        searchQuery={searchQuery}
                        simplifiedFilter={simplifiedFilter}
                        setSimpFilter={setSimpFilter}
                        setSearch={setSearch} /> 
        
        
        : <AdvancedSearch fieldHelp={fieldHelp.fields} searchQuery={searchQuery} setSearch={setSearch} ind={0} />}
        {simpleSearch
        ? <Form.Row className="justify-content-center mt-1">
            <Button variant="link" onPointerDown={() =>{
                    simpToNormalSearch(simplifiedFilter)
                    setSimpleSearch(false)
                } 
                }>
                Consulta avanzada
            </Button>
            </Form.Row>
        :<Form.Row className="justify-content-center mt-1">
            <Button variant="link" onPointerDown={() => {
                setSearch(initialQuery)
                setSimpleSearch(true)}}>
                Consulta simple
            </Button>
        </Form.Row>
        }

        <Form.Row className="justify-content-center mt-1 mb-4">
            <Button variant="link">
                Buscar
            </Button>
        </Form.Row>
        </>
    )
}

export default GenericSearch