import { Form, Col, Row, Button } from "react-bootstrap"
import sub from 'date-fns/sub'

import DateRangeComponent from './DateRangeComponent'
import AggregationField from "./AggregationField"
import PropTypes from 'prop-types';
import { useState } from "react"
import SubsequenceCanvas from "./SubsequenceCanvas"



/**
 * Advanced form for create and update query data.
 * @param {searchQuery} searchQuery object for retrieving data
 * @param {setSearch} useState hook for setting data
 * @param ind for spawning more than one advanced search at once
 * @returns 
 */
const AdvancedSearch = ({searchQuery,setSearch,ind}) => {
    const [subSearch,setSubSearch] = useState(
        searchQuery.subsequenceQuery !== undefined)

    console.log(searchQuery)

    const removeObjectFromList = (ind,field) => {
        const newSearch = {...searchQuery, [field]: searchQuery[field].filter( (range,index) => index != ind)}
        setSearch(newSearch)
    }

    const setField = (field,value) => {
        setSearch({
            ...searchQuery, [field]: value
        })
    }
    const setObjectArrayField = (field, arrayVal, value, index) => {
        let newNestedArray = searchQuery[arrayVal]
        newNestedArray[index] = {...newNestedArray[index], [field]:value}
        setSearch({
            ...searchQuery, [arrayVal]: newNestedArray
        })
    }
    return(
        <Form>
            <Form.Group controlID="filterField">
                <Form.Label>Filtro SQL</Form.Label>
                <Form.Control type="text"
                            as="textarea"
                            onChange={e => setField('filter',e.target.value)}
                            defaultValue={searchQuery.filter? searchQuery.filter: "city = 'Granada'"} />
                <Form.Text className="text-muted">
                    Campos disponibles: sensorID, measure, measure_name, unit, name, measure_desc, city, region, country, address, description, tags, lat, long
                </Form.Text>
            </Form.Group>
            
            <DateRangeComponent searchQuery={searchQuery}
                                setSearch={setSearch}
                                removeObjectFromList={removeObjectFromList} 
                                setObjectArrayField={setObjectArrayField}
                                />

            <AggregationField aggFields={searchQuery.aggregationFilter} setAggField={setObjectArrayField} 
                                removeAggField={removeObjectFromList} setNewAgg={setField}
            />

            <Form.Row className="justify-content-center">
                <Form.Group as={Row} className="ml-1">
                    <Form.Label>
                        Devolver series temporales
                    </Form.Label>
                <Col>
                    <Form.Check type="checkbox" 
                        checked={searchQuery.timeseries} 
                        onChange={() => {
                            setSubSearch(false)
                            setField('timeseries', !searchQuery.timeseries)}} />
                </Col>
                </Form.Group>
                <Form.Group as={Row} className="ml-1">
                    <Form.Label>
                        Buscar por subsecuencias
                    </Form.Label>
                    <Col>
                        <Form.Check type="checkbox" 
                            checked={subSearch}
                            disabled={!searchQuery.timeseries}
                            onChange={() => {
                                setField('timeseries', true)
                                setSubSearch(!subSearch)
                                if (!subSearch)
                                    setField('subsequenceQuery',undefined)
                                }} />
                    </Col>
                </Form.Group>
            </Form.Row>
            {subSearch && <SubsequenceCanvas ind={ind}subsequence={searchQuery.subsequenceQuery?
                            searchQuery.subsequenceQuery.subsequence : []}
                            setSubsequence={setField} 
                            />
            }
        </Form>
    )
}

AdvancedSearch.propTypes = {
    initSearchQuery :PropTypes.shape({
        limit: PropTypes.number.isRequired,
        timeseries: PropTypes.bool.isRequired,
        timeRange: PropTypes.arrayOf(PropTypes.shape({
            lowerBound: PropTypes.date,
            upperBound: PropTypes.date
            }
        )).isRequired,
        type: PropTypes.string.isRequired,
        filter: PropTypes.string.isRequired,
        subsequenceQuery: PropTypes.shape({
            subsequence: PropTypes.arrayOf(PropTypes.number)
        }),
        aggregationFilter: PropTypes.arrayOf(PropTypes.shape({
            operation: PropTypes.string,
            aggComparation: PropTypes.string,
            value: PropTypes.number,
            relation: PropTypes.string
            }
        ))
    })
}


export default AdvancedSearch

