import { useEffect, useState } from "react"
import { Form, Button } from "react-bootstrap"
import sub from 'date-fns/sub'
import AdvancedSearch from "./AdvancedSearch"
import SimpleSearch from "./SimpleSearch"
import Link from "next/link"

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
    "city": "none",
    "region": "none",
    "country": "none",
    "names": "none",
    "measure_name": "none",
    "sampling_unit": "none",
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
    const [validated, setValidated] = useState(false);


    const handleSubmit = (event) => {
        const form = event.currentTarget;
        
        if (form.checkValidity() === false) {
          event.preventDefault();
          event.stopPropagation();
        }
        else if (searchQuery.subsequenceQuery){
            if (searchQuery.subsequenceQuery.subsequence.length < 3){
                event.preventDefault();
                event.stopPropagation();
            }
        }

        if (searchQuery.subsequenceQuery){
            setSearch({...searchQuery, type: "complex"})
        }
        else if (searchQuery.aggregationFilter){
            setSearch({...searchQuery, type: "aggregation"})
        }

    
        setValidated(true);
      };



    const simpToNormalSearch = (simplifiedSearch) => {
        let sqlFilter = ""
        for (const key in simplifiedSearch){
            if (simplifiedSearch[key] ){
                if (sqlFilter !== "" && simplifiedSearch[key] !== "none" ){
                    sqlFilter = sqlFilter.concat( " AND ")
                }
                if (key == 'tags')
                {   if (simplifiedSearch.tags.size > 0){
                    const tags = [...simplifiedSearch[key]]
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
    
    useEffect(() => simpToNormalSearch(simplifiedFilter), [simplifiedFilter])

    return(
        <Form noValidate validated={validated} onSubmit={handleSubmit}>
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
            <Link href={{pathname: "/search",  query: { query: JSON.stringify(searchQuery)}}}>
                <Button type="submit" variant="link">
                    Buscar
                </Button>
            </Link>
        </Form.Row>
        </Form>
    )
}

export default GenericSearch