import { Container } from 'react-bootstrap'
import dynamic from 'next/dynamic'
import { Form, Col, Row } from "react-bootstrap"
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
    const [fileVal, setValidFile] = useState({msg: "", valid: null})

    const handleFileInput = (e) => {
        const fileToRead = e.target.files[0]
        const fileReader = new FileReader()

        fileReader.addEventListener('load', (fileLoadedEvent) => {
            const textFromFileLoaded = fileLoadedEvent.target.result

            try{
                let jsonParse = JSON.parse(textFromFileLoaded)
                if (jsonParse.hasOwnProperty('subsequence')){
                    let values = jsonParse['subsequence']
                    if (values.length > 3 ){
                    //Aplicamos normalización min-max para poder meterlo bien en el gráfico
                    //Esto no debería de afectar a la búsqueda por que en esta aplcian antes una z-normalizacion
                        let max = Math.max(...values)
                        let min = Math.min(...values)
                        let normalized_values = values.map(el => (el-min)/(max-min))
                        setSubsequence('subsequenceQuery',{subsequence: normalized_values})
                        setValidFile({msg: `Archivo ${fileToRead.name} cargado `,valid: true})
                    }
                    else{
                        setValidFile({msg: `Campo subsequence no encontrado en ${fileToRead.name}`, valid: false})
                    }


                }
                else{
                    setValidFile({msg: `Campo subsequence debería de tener al menos 3 valores${fileToRead.name}`, valid: false})
                }
            }catch(error){
                
                setValidFile({msg:`No se ha podido parsear ${fileToRead.name}, comprueba el formato`,valid: false})
            }
          }) //la lectura es asíncrona
      
        fileReader.readAsText(fileToRead, 'UTF-8')
    }

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
        
        
        if (data.points){
            const newSubsequence = [...subsequence]
            data.points.forEach( (p) => newSubsequence.splice(p.x,1))
            setSubsequence('subsequenceQuery', {subsequence: newSubsequence}) 
        }
    

    }


    return(
    <>
        <Row className="mb-3">
            <Col>
                <Form.File id="subseq-file" onChange={(e) => handleFileInput(e)}
                feedback={fileVal.msg}
                isValid={fileVal.valid}
                isInvalid={!fileVal.valid}
                data-browse="Subir"
                label="Sube un archivo json con: {subsequence: [1,3,2,5]}"
                custom>        
                </Form.File>
            </Col>
        </Row>
    
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
                custom
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
                    custom
                    onChange={() => {
                    setDeletePoints(true)}}
                 />
            </Col>
        </Form.Group>
        <Col>
            <Form.Text className="text-muted pt-0 mt-0">
            Añade puntos para reflejar la forma de la subsecuencia que quieres. Pon al menos tres
            </Form.Text>
        </Col>
    </Form.Row>
    {
        subsequence.length < 3 && <Form.Row className="mb-1"><Form.Text className="text-danger">Pon al menos 3 puntos en la subsecuencia</Form.Text></Form.Row>
        }


    
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