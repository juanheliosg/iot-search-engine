import {Card, Row, Col} from 'react-bootstrap'
import TimeSeriesChart from './TimeSeriesChart'

const ResultCard = ({serie,subLen}) =>{
    return(
        <Card className="mt-4">
            <Card.Body>
                <Row>
                    <Col xs={2}>
                        <p>SensorId</p>
                    </Col>
                    <Col xs={2}>
                        <p className="font-weight-bold">{serie.sensorId}</p>
                    </Col>

                    <Col xs= {2}>
                        <p>Fuente</p>
                    </Col>
                    <Col xs={2}>
                        <p className="font-weight-bold">{serie.sourceName}</p>
                    </Col>
                    <Col>
                        <p>{serie.description}</p>
                    </Col>
                </Row>
                <Row>
                    <Col xs={2}>
                        <p>Medición</p>
                    </Col>
                    <Col xs={2}>
                        <p className="font-weight-bold">{serie.measureName}</p>
                    </Col>
                    <Col xs={2}>
                        <p>Medida</p>
                    </Col>
                    <Col xs={2}>
                        <p className="font-weight-bold">{serie.measureName}</p>
                    </Col>
                    <Col>
                        <p>{serie.measureDescr}</p>
                    </Col>
                </Row>
                <Row>
                    <Col>
                        <p>Ciudad</p>
                    </Col>
                    <Col>
                        <p className="font-weight-bold">{serie.city}</p>
                    </Col>
                    <Col>
                        <p>Region</p>
                    </Col>
                    <Col>
                        <p className="font-weight-bold">{serie.region}</p>
                    </Col>
                    <Col>
                        <p>País</p>
                    </Col>
                    <Col>
                        <p className="font-weight-bold">{serie.country}</p>
                    </Col>
                    <Col>
                        <p>Coordenadas: </p>
                    </Col>
                    <Col>
                        <p className="font-weight-bold">[{serie.lat},{serie.long}]</p>
                    </Col>
                    <Col>
                        <p>Direccion</p>
                    </Col>
                    <Col>
                        <p className="font-weight-bold" >{serie.address}</p>
                    </Col>
                </Row>
                <Row>
                    {serie.stats.map( el =>
                    <>
                    <Col xs={2}>
                       <p>{el.name}</p>
                    </Col>
                    <Col xs={2}>
                        <p className="font-weight-bold">{parseFloat(el.value).toFixed(3)}</p>
                    </Col>
                    </>
                       )}
                </Row>
                {serie.timestamps.length > 0 && serie.values.length > 0 && 
                <TimeSeriesChart timestamps={serie.timestamps} 
                values={serie.values} 
                subseq={serie.subsequences} 
                subLen={subLen}
                measureName={serie.measureName} />
                }

                
            </Card.Body>
        </Card>

    )
}

export default ResultCard