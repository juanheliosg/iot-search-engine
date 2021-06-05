import PropTypes from 'prop-types';
import { useState, forwardRef } from 'react'
import DatePicker from "react-datepicker"
import { Form, Col, Row, Button } from "react-bootstrap"
import { es } from "date-fns/locale"
import PlusCircleFill from "./icons/Plus"

const CustomInput = forwardRef(
    ({ value, onClick }, ref) => (
      <Button style={{}} className="btn-xs" variant="outline-primary" onClick={onClick} ref={ref}>
        {value}
      </Button>
    ),
  );

/**
   * DateRange field for view and update dateranges
   * @param endDate initial end date
   * @param startDate initial start date
   * @param setRanges setRanges function for updating the query object
   * @param ind index for this field 
   * @returns 
*/
const DateRangeField =  ({endDate, startDate,ind, setRanges}) =>{
    return(
    <>
        <Form.Group controlId="lowerBoundField">
            <Row>
                <Form.Label className="align-self-center" as={Col}>
                    Desde
                </Form.Label>
                <Col md="auto">
                    <DatePicker className="text-center" selected={startDate} onChange={(date) => {
                        if (date < endDate){
                            setRanges('lowerBound','timeRange',date,ind)
                            } 
                        }
                    } 
                    timeInputLabel="Time:"
                    dateFormat="dd/MM/yyyy-HH:mm" locale={es} showTimeInput customInput={<CustomInput />} />
                </Col>
            </Row>
        </Form.Group>
        <Form.Group className="ml-4 pl" controlId="upperBoundField">
            <Row>
                <Form.Label className="align-self-center" as={Col}>
                    Hasta
                </Form.Label>
                <Col md="auto">
                    <DatePicker className="text-center" selected={endDate} onChange={(date) =>{
                        if (date > startDate){
                            setRanges('upperBound','timeRange',date,ind)} 
                        }
                    } timeInputLabel="Time:"
                            dateFormat="dd/MM/yyyy-HH:mm" locale={es} showTimeInput customInput={<CustomInput />}
                     />
                </Col>
            </Row>
        </Form.Group>
    </>
    )
}


export default DateRangeField