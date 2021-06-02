import { Form, Col, Row, Button } from "react-bootstrap"
import PlusCircleFill from "./icons/Plus"
import DashCircleFill from "./icons/Minus"
import {useState} from 'react'
import { createPortal } from "react-dom"

/**
 * Aggregation field form component representing and aggregation filter
 * 
 * @param {aggFields} aggregation field
 * @param {setNewAgg} function for setting a new agg filter in the query
 * @param {setAggField} function for setting a value in an agg filter object
 * @param {removeAggField} function for removing an agg field
 * @returns 
 */
const AggregationFieldSearch = ({aggFields, setNewAgg, setAggField, removeAggField}) => {
    const [compAgg, setCompAgg] = useState(false)
    const [compValue, setCompValue] = useState(false)

    const addNewAggFilter = () =>{
        const newAgg = {
            operation: "avg",
            aggComparation: compAgg? "avg" : null,
            value: compValue? 1 : null,
            relation: compValue || compAgg? ">=" : null
        
        }
        let newAggFields = []
        if (aggFields){
            newAggFields = [...aggFields, newAgg]
        }
        else{
            newAggFields =[newAgg]
        }
        
        console.log(newAggFields)
        setNewAgg('aggregationFilter',newAggFields)
    }
     
    return(
        <>
            <Row className="align-items-center justify-content-between">
                    <Col xs={3} >
                        <p className="mb-0" style={{fontSize: "15px"}}>Filtro agregación</p>
                    </Col>
                    <Col xs={3} className="pl-0">
                        <p className="mb-0" style={{fontSize: "15px"}}>Comparar agregaciones</p>     
                    </Col>
                    <Col xs={1} className="pl-0">
                        <input type="checkbox" value={false}  checked={compAgg} onChange={() => {
                                                                    if(!compAgg && compValue){
                                                                        setCompValue(!compValue)
                                                                    }

                                                                     setCompAgg(!compAgg)
                                                                    }
                                                                }
                                                               />
                    </Col>
                    <Col xs={3} className="pl-0">
                        <p className="mb-0"   style={{fontSize: "15px"}}>Comparar con valor</p>
                    </Col>
                    <Col xs={1} className="pl-0">
                        <input type="checkbox" value={false} checked={compValue} onChange={() => 
                                                                {
                                                                    if(compAgg && !compValue){
                                                                        setCompAgg(!compAgg)
                                                                    }
                                                                        
                                                                    setCompValue(!compValue)
                                                                    }}/>
                    </Col>
                    
                    <Col xs={1} className="pl-0">
                        <Button variant="link" onClick={ () => addNewAggFilter()}>
                            <PlusCircleFill />
                        </Button>
                    </Col>  
            </Row>
            <Form.Row>
                <p style={{fontSize: "15px"}} className="mb-0 ml-2 mb-4 text-muted">
                    Las consultas de agregación permitir obtener y
                     comparar medidas estadísticas como la media de una serie temporal 
                     con medidas estadísticas del total de las series filtradas o con el valor que pongas.
                    
                    Marca las cajas para comparar con agregacioneso valor o no marques ninguna para obtener solo la medida
                </p>
            </Form.Row>
            {
                aggFields && aggFields.map( (agg,ind) => {
                    return(
                    <Form.Row className="justify-content-between">
                        <Form.Group as={Row}>
                            <Form.Label  className="mr-0" xs={5} column>
                                 Series con
                            </Form.Label>
                            <Col className="mr-0 ml-0 pl-0">
                                <Form.Control as="select" defaultValue={agg.operation} 
                                    onChange={(e) => setAggField('operation','aggregationFilter',e.target.value, ind)}
                                    custom
                                >
                                    <option value= "avg">media</option>
                                    <option value= "std">desviación estándar</option>
                                    <option value= "sum">suma</option>
                                    <option value= "max">máximo</option>
                                    <option value= "min">mínimo</option>
                                </Form.Control>
                            </Col>
                        </Form.Group>
                    {(agg.value !== null || agg.aggComparation) && 
                    
                        <Form.Group as={Col} xs={2}>
                                <Form.Control as="select" defaultValue={agg.relation}  
                                    onChange={(e) => setAggField('relation','aggregationFilter',e.target.value, ind)}
                                    custom>
                                    <option value= ">">{">"}</option>
                                    <option value= "<">{"<"}</option>
                                    <option value= ">=">{">="}</option>
                                    <option value= "<=">{"<="}</option>
                                    <option value= "=">{"="}</option>
                                </Form.Control>
                        </Form.Group>
                    }
                    {(agg.value !== null && agg.value !== undefined) && 
                        <Form.Group as={Col} >          
                            <Form.Control type="number" defaultValue={agg.value} 
                                onChange={(e) => {setAggField('value','aggregationFilter',e.target.value, ind)}}
                                /> 
                        </Form.Group>
                    }
                    {agg.aggComparation &&
                        <Form.Group as={Col} className="mr-0 ml-0 pl-0" >
                            <Form.Control as="select" defaultValue={agg.operation} 
                            onChange={(e) => setAggField('aggComparation','aggregationFilter',e.target.value, ind)}
                            custom>
                                <option value= "avg">media total</option>
                                <option value= "std">desviación estándar total</option>
                                <option value= "sum">suma total</option>
                                <option value= "max">máximo total</option>
                                <option value= "min">mínimo total</option>
                            </Form.Control>
                        </Form.Group>
                    }
                            <Col xs={1} className="ml-1">
                                <Button variant="link" onClick={() => removeAggField(ind,'aggregationFilter')}>
                                     <DashCircleFill />
                                </Button>
                            </Col>
                    </Form.Row>
                    )
                })
            }
        </>

    )
}

export default AggregationFieldSearch