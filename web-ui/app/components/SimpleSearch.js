import { Form, Col, Row, Button, Badge } from "react-bootstrap"
import {useState} from 'react'
import PlusCircleFill from "./icons/Plus"
import DashCircleFill from "./icons/Minus"
import AggregationField from "./AggregationField"
import DateRangeComponent from './DateRangeComponent'

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




const SimpleSearch = ({searchQuery, setSearch, simplifiedFilter,setSimpFilter}) => {
    const [tagSelect, setTagSelect] = useState("traffic") 

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

    return(
        <Form>
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
                                <option value="traffic">traffic</option>
                                <option value="environment">environment</option>
                                <option value="smartcity">smartcity</option>
                                <option value="industry">industry</option>
                                <option value="noise">noise</option>
                                <option value="lights">lights</option>
                        </Form.Control>
                    </Col>
                    <Col xs={1}>
                        <Button variant="link"
                            onPointerDown={(e) => {
                            const newTags = new Set(simplifiedFilter.tags).add(tagSelect)
                            setObjectField('tags',newTags, simplifiedFilter,setSimpFilter)}
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
                    <Form.Control type="text"
                        onChange={e => 
                        setObjectField('city',e.target.value, simplifiedFilter, setSimpFilter)}
                        placeholder="Santander" />
                </Col>
                <Form.Label xs={2} className="pr-0" column>Region</Form.Label>
                <Col className="pr-0 pl-0" xs={4}>
                    <Form.Control type="text"
                        onChange={e => 
                        setObjectField('region',e.target.value, simplifiedFilter, setSimpFilter)}
                        placeholder="Cantabria" />
                </Col>
            </Form.Group>
            <Form.Group as={Row}>
                <Form.Label className="pr-0" xs={2} column>País</Form.Label>
                <Col xs={4} className="pr-0" >
                    <Form.Control type="text"
                        onChange={e => 
                        setObjectField('country',e.target.value, simplifiedFilter, setSimpFilter)}
                        placeholder="Spain" />
                </Col>
                <Form.Label className="pr-0" column>Dirección</Form.Label>
                <Col xs={4} className="pr-0 pl-0">
                    <Form.Control type="text"
                        onChange={e => 
                        setObjectField('address',e.target.value, simplifiedFilter, setSimpFilter)}
                        placeholder="Calle Manzanares"
                            />
                </Col>
            </Form.Group>
            
            <Form.Group as={Row} className="align-items-center">
                <Form.Label  md="auto" column>Medición</Form.Label>
                <Col md="auto" className="pr-1 pl-1">
                    <Form.Control  as="select"  value={simplifiedFilter.measure_name} 
                            onChange={(e) => setObjectField('measure_name',e.target.value, simplifiedFilter, setSimpFilter)}
                            custom
                        > 
                                <option value={null}>Cualquiera</option>
                                <option value="ruido">ruido</option>
                                <option value="luminosidad">luminosidad</option>
                                <option value="ocupacion">ocupacion</option>
                        </Form.Control>
                </Col>
                <Form.Label md="auto" className="pr-1 pl-1" column>Muestreo</Form.Label>
                <Col md="auto" className="pr-0 pl-1 mt-1">
                    <Form.Control  as="select"  value={simplifiedFilter.sampling_freq} 
                            onChange={(e) => 
                                setObjectField('sampling_freq',e.target.value, simplifiedFilter, setSimpFilter)}
                            custom
                        > 
                                <option value={null}>Na</option>
                                <option value={1}>1</option>
                                <option value={2}>2</option>
                                <option value={3}>3</option>
                        </Form.Control>
                </Col>
                <Col md="auto" className="pr-0 pl-2 mt-1">
                    <Form.Control  as="select"  value={simplifiedFilter.sampling_unit} 
                            onChange={(e) => setObjectField('sampling_unit',e.target.value, simplifiedFilter, setSimpFilter)}
                            custom
                        > 
                                <option value={null}>Cualquiera</option>
                                <option value="minuto">minuto</option>
                                <option value="segundo">segundo</option>
                        </Form.Control>
                </Col>
                <Form.Label md="auto" className="pr-0 " column>Devolver serie</Form.Label>
                <Col>
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
        </Form>

    )
}

export default SimpleSearch