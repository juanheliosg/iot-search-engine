import { Form, Col } from "react-bootstrap"
import sub from 'date-fns/sub'
import DatePicker from "react-datepicker"
import { useState } from 'react'

const AdvancedSearch = ({searchQuery}) => {
    const [startDate, setStartDate] = useState(new Date());
    let currentDate = new Date()
    let lastDayDate = sub(currentDate,{days: 1})
    return(
        <Form>
            <Form.Group controlID="filterField">
                <Form.Label>Filtro SQL</Form.Label>
                <Form.Control type="text" defaultValue={searchQuery.filter? searchQuery.filter.replace(): "city = 'Granada'"} />
                <Form.Text className="text-muted">
                    Campos disponibles: sensorID, measure, measure_name, unit, name, measure_desc, city, region, country, address, description, tags, lat, long
                </Form.Text>
            </Form.Group>
            <Form.Row>
                <Form.Group as={Col} controlId="lowerBoundField">
                    <Form.Label>
                        Desde
                    </Form.Label>
                    <Col xs="auto">
                        <Form.Control type="date" defaultValue={{currentDate}} />
                    </Col>
                </Form.Group>
                <Form.Group as={Col} controlId="filterField">
                    <Form.Label>
                        Hasta
                    </Form.Label>
                    <Col xs="auto">
                        <DatePicker selected={startDate} onChange={(date) => setStartDate(date)} timeInputLabel="Time:"
      dateFormat="MM/dd/yyyy H:m:s" locale="es-ES"
      showTimeInput/>
                    </Col>
                </Form.Group>
            </Form.Row>
        </Form>
    )
}

export default AdvancedSearch

