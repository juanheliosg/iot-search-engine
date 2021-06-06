import { Form, Col, Row, Button, Badge } from "react-bootstrap"
import {useState} from 'react'
import PlusCircleFill from "./icons/Plus"
import DashCircleFill from "./icons/Minus"
import AggregationField from "./AggregationField"
import DateRangeComponent from './DateRangeComponent'

const initialFilter = {
    "tags": new Set(),
    "city": "none",
    "region": "none",
    "country": "none",
    "address": "none",
    "measure_name": "none",
    "sampling_unit": "none",
    "sampling_freq": null
}




const SimpleSearch = ({searchQuery, setSearch, simplifiedFilter,setSimpFilter, fieldHelp}) => {

    const fieldHelpList = fieldHelp.fields

    const [tagSelect, setTagSelect] = useState("none") 

    const setObjectField = (field,value,object,setObject) => {
        setObject({...object, [field] : value})
    }

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

    const getFieldHelpList = (field) =>{
        return ( 
        fieldHelpList[field].isLoading || fieldHelpList[field].error?
            <option value="">Cargando...</option> :
        fieldHelpList[field].fieldList.map( tag =>
            <option value={tag}>{tag}</option>
        )
        )
    }


    return(
        <>
            <Form.Group>
                <Form.Row className="align-items-center mb-2">
                    <Form.Label className="mb-0">Tags</Form.Label>
                    <Form.Text className="ml-3 text-muted">
                        Añade los tags con los que quieres que esten relacionados los sensores
                    </Form.Text>
                </Form.Row>

                <Form.Row className="align-items-center mb-2">
                    <Col xs={11}>
                        <Form.Control  as="select"  value={tagSelect}
                            onChange={(e) => setTagSelect(e.target.value)}
                            custom
                        >     
                                {getFieldHelpList("tags")}
                                <option value="none">Cualquiera</option>
                        </Form.Control>
                    </Col>
                    <Col xs={1}>
                        <Button variant="link"
                            disabled={simplifiedFilter.tags.length == 0}
                            onPointerDown={(e) => {
                                if(tagSelect !== "none"){
                                    const newTags = new Set(simplifiedFilter.tags).add(tagSelect)
                                    setObjectField('tags',newTags, simplifiedFilter,setSimpFilter)}
                            }
                            } 
                            >
                                <PlusCircleFill />
                        </Button>
                    </Col>                   
                </Form.Row>
                <Form.Row className="justify-content-around">
                {
                    [...simplifiedFilter.tags].map( (el,ind) => {
                        return(
                    
                    <Button as={Col} md="auto" className="d-flex pr-0 pl-0 mt-2 ml-2" variant="primary" 
                            onClick={(e) => {
                                const newTags = new Set(simplifiedFilter.tags)
                                if (newTags.delete(el)){
                                    setObjectField('tags', newTags,simplifiedFilter, setSimpFilter)
                                    }
                                }}>
                            <Col md="auto">
                                <p className="pb-0 mb-0">{el}</p>
                            </Col>
                            <Col className="pl-1">
                                <DashCircleFill size={"0.8em"}/>
                            </Col>    
                        </Button>   
                    )
                })}
                </Form.Row>
            </Form.Group>
            
            <Form.Group as={Row} className="justify-content-center">
                <Form.Label className="pr-0" xs={2} column>Ciudad</Form.Label>
                <Col className="pr-0" xs={4}>
                    <Form.Control as="select" value={simplifiedFilter.city}
                        onChange={e => 
                        setObjectField('city',e.target.value, simplifiedFilter, setSimpFilter)}
                        placeholder="Santander">
                            {getFieldHelpList("cities")}
                            <option value="none">Cualquiera</option>
                        </Form.Control>
                </Col>
                <Form.Label xs={2} className="pr-0" column>Region</Form.Label>
                <Col className="pr-0 pl-0" xs={4}>
                    <Form.Control as="select" value={simplifiedFilter.region}
                        onChange={e => 
                        setObjectField('region',e.target.value, simplifiedFilter, setSimpFilter)}>
                            {getFieldHelpList("regions")}
                            <option value="none">Cualquiera</option>
                            
                        </Form.Control>
                </Col>
            </Form.Group>
            <Form.Group as={Row}>
                <Form.Label className="pr-0" xs={2} column>País</Form.Label>
                <Col xs={4} className="pr-0" >
                    <Form.Control as="select" value={simplifiedFilter.country}
                        onChange={e => 
                        setObjectField('country',e.target.value, simplifiedFilter, setSimpFilter)}
                        >
                            {getFieldHelpList("countries")}
                            <option value="none">Cualquiera</option>
                        </Form.Control>
                </Col>
                <Form.Label className="pr-0" column>Fuente</Form.Label>
                <Col xs={4} className="pr-0 pl-0">
                    <Form.Control as="select" value={simplifiedFilter.name}
                        onChange={e => 
                        setObjectField('address',e.target.value, simplifiedFilter, setSimpFilter)}>
                        {getFieldHelpList("names")}
                        <option value="none">Cualquiera</option>

                        </Form.Control>
                </Col>
            </Form.Group>
            
            <Form.Group as={Row} className="align-items-center justify-content-between">
                <Form.Label md="auto" className=" pr-1" column>Medición</Form.Label>
                <Col md="auto" className="pr-1 pl-0">
                    <Form.Control  as="select"  value={simplifiedFilter.measure_name} 
                            onChange={(e) => setObjectField('measure_name',e.target.value, simplifiedFilter, setSimpFilter)}
                            custom
                        > 
                            {getFieldHelpList("measure")}
                            <option value="none">Cualquiera</option>
                        </Form.Control>
                </Col>
                <Form.Label md="auto" className="pr-0 pl-1" column>Muestreo</Form.Label>
                <Col  className="pr-0 pl-1 mt-1">
                    <Form.Control  type="number"  value={simplifiedFilter.sampling_freq} 
                            onChange={(e) => 
                                setObjectField('sampling_freq',e.target.value, simplifiedFilter, setSimpFilter)}
                    
                        > 
                        </Form.Control>
                </Col>
                <Col md="auto" className="pr-0 pl-2 mt-1">
                    <Form.Control  as="select"  value={simplifiedFilter.sampling_unit}  value={simplifiedFilter.sampling_unit}
                            onChange={(e) => setObjectField('sampling_unit',e.target.value, simplifiedFilter, setSimpFilter)}
                            custom
                        > 
                                {getFieldHelpList("sample_units")}
                                <option value="none">Cualquiera</option>
                        </Form.Control>
                </Col>
                <Form.Label md="auto" className="pr-0 pl-1" column>Devolver serie</Form.Label>
                <Col xs={1}>
                    <Form.Check type="checkbox" 
                            checked={searchQuery.timeseries}
                            onChange={() => {
                                setObjectField('timeseries', !searchQuery.timeseries, searchQuery, setSearch )
                                }}
                                 />
                </Col>
            </Form.Group>
            <DateRangeComponent searchQuery={searchQuery}
                                setSearch={setSearch}
                                removeObjectFromList={removeObjectFromList} 
                                setObjectArrayField={setObjectArrayField}
                                />

            <AggregationField aggFields={searchQuery.aggregationFilter} setAggField={setObjectArrayField} 
                                removeAggField={removeObjectFromList} setNewAgg={setField}
            />
        </>

    )
}

export default SimpleSearch