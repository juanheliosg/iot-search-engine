import { Form, Col, Row, Button } from "react-bootstrap"
import sub from 'date-fns/sub'

import PlusCircleFill from "./icons/Plus"
import DashCircleFill from "./icons/Minus"
import DateRangeField from "./DateRangeField"

const DateRangeComponent = ({searchQuery, setSearch, removeObjectFromList, setObjectArrayField}) => {

    const addRange = () =>  {
        let currentDate = new Date()
        let lastDayDate = sub(currentDate,{days: 1})
        const newTimeRanges = [...searchQuery.timeRange,{
            lowerBound: lastDayDate,
            upperBound: currentDate
        }]
        
        const newSearch = {...searchQuery, timeRange: newTimeRanges}
        setSearch(newSearch)
    }

    return(
    <>
    {searchQuery.timeRange.map((range, index) => {
        return(
            <Form.Row key={index} className="justify-content-center">
                <DateRangeField startDate={range.lowerBound}
                            endDate={range.upperBound}
                            ind={index}
                            key={index}
                            setRanges={setObjectArrayField}
                            />
                { index > 0?
                    <Col xs={1} className="ml-1">
                        <Button variant="link" onClick={() => removeObjectFromList(index,'timeRange')}>
                         <DashCircleFill />
                        </Button>
                    </Col>
                    :         
                    <Col xs={1} className="ml-1" onClick={() => addRange()}>
                        <Button variant="link">
                         <PlusCircleFill />
                        </Button>
                    </Col>
                }
            </Form.Row>
            )
        })
    }
    </>
    )
}

export default DateRangeComponent