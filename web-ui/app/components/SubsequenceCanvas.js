import { Container } from 'react-bootstrap'
import dynamic from 'next/dynamic'
import { Form, Col, Row, Button } from "react-bootstrap"
import { useRef, useState } from 'react'

const DynamicPlot = dynamic(() => import(
    '../../node_modules/react-plotly.js/react-plotly'
    ),
    {ssr: false})

/**
* Plotly chart wrapper with functionality for adding and removing points  
* @param {subsequence, setSubsequence} subsequence to be send and function for setting the subsequence in the query
* @returns 
*/
const SubsequenceCanvas = ({subsequence, setSubsequence}) => {
    
    const containerRef = useRef(null)
    const  [deletePoints,setDeletePoints] = useState(false)

    const newPoint = (e) => {
        //Está dentro del área de selección

        const getTopBox = () => {
            //Manera bastante hacky de sacar los limites inferiores y superiores del gráfico además de los laterales derechos
            //basicamente nos cogemos un elemento permanente del grafico de plotly que cumpla esa función
            //En este caso es la línea asociada a la rejilla que atraviesa el 1
            return containerRef.current.getElementsByClassName("gridlayer")[0].children[1].getBBox()
        }
        const getBottomLineHeight = () => {
            //Aqui el elemento del bottom es el tick label de 0 (no se puede hacer con la botom line)
            //Cómo hace un rectángulo lo que hacemos es que nos quedamos con la mitad de este rectángulo
            let rectangle = containerRef.current.getElementsByClassName("yaxislayer-above")[0].children[0].getBBox()
            return rectangle.y + rectangle.height/2
        }
        if (e.target.classList[2] === "cursor-pointer" ){
            //Buscar lugar donde insertar valor
            let topBox = getTopBox()
            let topLine = topBox.y
            let bottomLine = getBottomLineHeight()
       
            let newY = 1-(e.nativeEvent.offsetY-topLine)/ (bottomLine-topLine)

            if (newY > 1){
                newY = 1
            }   
            else if (newY < 0){
                newY = 0
            }
  
            if (subsequence.length > 0){
                let intervalLength =  topBox.width/subsequence.length
                //insertIndex representa la posición en la que hay que insertar
                //el nuevo punto 
                let insertIndex = Math.floor((e.nativeEvent.offsetX+ topBox.x)/intervalLength)
                const newSubsequence = [...subsequence] 
                newSubsequence.splice(insertIndex,0,newY)
                setSubsequence('subsequenceQuery',{subsequence: newSubsequence}) 
            }
            else{
                setSubsequence('subsequenceQuery',{subsequence: [newY]})
            }
          
 
        }
    }

    const removePoint = (data) => {
        
        console.log(data)
        if (data.points){
            const newSubsequence = [...subsequence]
            data.points.forEach( (p) => newSubsequence.splice(p.x,1))
                
            setSubsequence('subsequenceQuery', {subsequence: newSubsequence}) 
        }
    

    }


    return(
    <>
        <Form.Row>
        <Form.Group as={Row} className="ml-1">
            <Form.Label>
                Añadir
            </Form.Label>
        <Col>
            <Form.Check type="radio" 
                checked={deletePoints === false}
                id="anadir"
                name="edition"
                onChange={() => {
                    setDeletePoints(false)}}
                    />
        </Col>
        </Form.Group>
        <Form.Group as={Row} className="ml-1">
            <Form.Label>
                    Borrar
            </Form.Label>
            <Col>
                <Form.Check type="radio" 
                    checked={deletePoints === true }
                    id="borrar"
                    name="edition"
                    onChange={() => {
                    setDeletePoints(true)}}
                 />
            </Col>
        </Form.Group>
        <Col>
        <Form.Text className="text-muted">
            Añade puntos para reflejar la forma de la subsecuencia que quieres buscar
        </Form.Text>
        </Col>

    </Form.Row>

    
    <Container ref={containerRef} onPointerDown={(e) => {if(!deletePoints) newPoint(e)}}>
        <DynamicPlot
        onClick={(e) => {if(deletePoints) removePoint(e) }}
        data={[
          {
            x: subsequence.map( (el,index) => index ),
            y: subsequence,
            type: 'lines',
            mode: 'lines+markers',
            marker: {color: 'blue'},
            
          }
        ]}
        config={{displayModeBar: false}}
        layout={{
            margin: {l:20,r:20,t:20,b:20},
            hovermode: "closest",
            xaxis: {
                fixedrange: true,
                showticklabels: false,
            },
            yaxis : {
                range: [-0.1,1.1],
                fixedrange: true,
                
            }
        }}
        useResizeHandler
        style={{width:"100%", height:"75%"}}
      />
    </Container>
    </>
    
)
}
export default SubsequenceCanvas