import { Form, Col, Row, Button, Overlay, Tooltip } from "react-bootstrap"
import sub from 'date-fns/sub'

import DateRangeComponent from './DateRangeComponent'
import AggregationField from "./AggregationField"
import PropTypes from 'prop-types';
import { useRef, useState } from "react"
import SubsequenceCanvas from "./SubsequenceCanvas"

const getHelp = (fieldHelpList) => {

    return [
        {field: "sensorID",
            msg: "Localiza un sensor único"},
        {field: "measure",
            msg: "Compara numéricamente para obtener sensores con valores mayores a una cantidad"},
        {field: "measure_name", msg: "Nombre de la medición",
            values: fieldHelpList?fieldHelpList['measure'].fieldList:[]},

        {field: "measure_units", msg: "Unidad física de la medición",
        values: fieldHelpList?fieldHelpList['measure_units'].fieldList:[]},

        {field: "name", msg: "Nombre de la fuente de datos",
        values: fieldHelpList?fieldHelpList['names'].fieldList:[]},
        {field: "measure_desc", msg:"Descripción, usa LIKE para buscar dentro"},

        {field: "city", msg: "Ciudad donde está la fuente", 
            values: fieldHelpList?fieldHelpList['cities'].fieldList:[] },

        {field: "region", msg: "Región donde está la fuente", 
        values: fieldHelpList?fieldHelpList['regions'].fieldList:[] },

        {field: "country", msg: "País donde está la fuente", 
        values: fieldHelpList?fieldHelpList['countries'].fieldList:[] },

        {field: "address", msg: "Dirección postal de la fuente"},

        {field: "description", msg: "Descripción de la fuente de datos. Usa Like para buscar dentro"},

        {field: "tags", msg: "Etiquetas sobre la fuente", 
        values: fieldHelpList?fieldHelpList['tags'].fieldList:[] }
    ]
}


const filterHelpOverlay = (fieldHelp) => {
    //diferenciar entre los que tienen información de campos y los que no
    //dar ayuda de los que no
    //hacer un filter fields objecto con la ayuda para cada uno e iterar a partir de ahí
    //añadir adicionalmente lso fields para cuando no se tenga la ayuda
    const [show, setShow] = useState(false);
    const target = useRef(null)
    if (fieldHelp.values !== undefined){
        return(
        <>
            <Button
                ref={target}
                onClick={() => setShow(!show)}
                variant="light"
                className="d-inline-flex align-items-center"
            >
                <span className="ml-1">{fieldHelp.field}</span>
            </Button>
            <Overlay placement="top-end" target={target.current} show={show}>
            {(props) => (
                <Tooltip id={fieldHelp.msg} {...props}>
                    <p>{fieldHelp.msg}</p> 
                    {fieldHelp.values.length == 0? <p>Cargando...</p>:
                    <>
                    <p>Valores posibles</p>
                    <p>{fieldHelp.values.map(el => `${el} `)}</p>
                    </>}
                </Tooltip>
            )}
            </Overlay>
        </>
)
    }
    else{
        return(
            <>
                <Button
                    ref={target}
                    onClick={() => setShow(!show)}
                    variant="light"
                    className="d-inline-flex align-items-center"
                >
                    <span className="ml-1">{fieldHelp.field}</span>
                </Button>
                <Overlay placement="top-end" target={target.current} show={show}>
                    <Tooltip>{fieldHelp.msg}</Tooltip>
                </Overlay>
            </>
        )
    }
}

/**
 * Advanced form for create and update query data.
 * @param {searchQuery} searchQuery object for retrieving data
 * @param {setSearch} useState hook for setting data
 * @param ind for spawning more than one advanced search at once
 * @returns 
 */
const AdvancedSearch = ({searchQuery,setSearch,ind, fieldHelp}) => {
    const [subSearch,setSubSearch] = useState(
        searchQuery.subsequenceQuery !== undefined)

    const helpList = getHelp(fieldHelp)
   


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
        <>
            <Form.Group>
                <Form.Label>Filtro SQL</Form.Label>
                <Form.Control type="text"
                            as="textarea"
                            onChange={e => setField('filter',e.target.value)}
                            value={searchQuery.filter}
                            placeholder={searchQuery.filter? searchQuery.filter: "city = 'Granada' AND measure > 8"} />
                <Form.Text className="text-muted">
                    Campos disponibles: {       
                        helpList.map( filterHelp => filterHelpOverlay(filterHelp))
                     }
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
                                if (!subSearch || searchQuery.timeseries == false)
                                    setField('subsequenceQuery',undefined)
                                }} />
                    </Col>
                </Form.Group>
            </Form.Row>
            {subSearch && <SubsequenceCanvas subsequence={searchQuery.subsequenceQuery?
                            searchQuery.subsequenceQuery.subsequence : []}
                            setSubsequence={setField} 
                            />
            }
        </>
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

