import dynamic from 'next/dynamic'
import {Row, Col} from 'react-bootstrap'
import { useState } from 'react'

const DynamicPlot = dynamic(() => import(
    '../../node_modules/react-plotly.js/react-plotly'
    ),
    {ssr: false})


/**
 * Component wrapper for a plotly chart representign a time series
 * @param {} param0 
 * @returns 
 */
const TimeSeriesChart = ({timestamps, values, subseq, subLen, measureName}) =>{
    let formattedTimestamps = timestamps.map(el => Date.parse(el))
    let data = [{
        x: formattedTimestamps,
        y: values,
        type: 'scatter',
        name: "Serie principal",
        mode: 'lines'
    },]

    subseq.sort( (a,b) => a.ed > b.ed? 1: -1)
    data = data.concat(subseq.map((sub,ind) => {return(
        {
            x: formattedTimestamps.slice(sub.start,subLen+sub.start),
            y: values.slice(sub.start,subLen+sub.start),
            type: "scatter",
            mode: 'lines',
            name: `Subseq ${ind} distancia euclidea:${parseFloat(sub.ed).toFixed(2)}`
        })}))

    let layout = {
        title: {
            text: "Serie temporal"
        },
        xaxis: {
            autorange: true,
            rangeselector: {buttons: [
                {
                count: 1,
                label: '1h',
                step: 'hour',
                stepmode: 'backward'
                },
                {
                count: 6,
                label: '6h',
                step: 'hour',
                stepmode: 'backward'
                },
                {step: 'all'}
            ]},
            rangeslider: {range: [formattedTimestamps[0], formattedTimestamps[formattedTimestamps.length -1]]},
            type: 'date'
        },
        yaxis: {
            autorange: true,
            type: 'linear',
            title:{
                text: `${measureName}`}
        }
        };
        
    
    return(
    <>
        {subLen > 0 && subseq[0] !== undefined && <Row>
            <Col>
                <p>Menor distancia euclidea a la secuencia:</p>
            </Col>
            <Col>
                <p className="font-weight-bold">{parseFloat(subseq[0].ed).toFixed(3)}</p>
            </Col>
            </Row>}
        <DynamicPlot 
            data={data}
            layout={layout}
            useResizeHandler
        style={{width:"100%", height:"100%"}}
        />
    </>
    )
}

export default TimeSeriesChart