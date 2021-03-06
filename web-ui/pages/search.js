
import { useQuery } from '../app/api/Query'
import { Accordion, Nav, Container,Row,Col, Pagination } from 'react-bootstrap'
import AccordionSearch from "../app/components/AccordionSearch";
import { useEffect, useState } from "react";
import SearchResults from "../app/components/SearchResults";
import Image from 'next/image'
import reformatRange from '../app/utils/ReformatRange'


const SearchHeader = ({isLoading}) => {
    return(
    <Container as="header">
        <Row className="justify-content-center">
            <Col className="justify-content-center d-flex mt-3">
                 <Image id={isLoading?"searchLoading":""} src="/logo.svg" width={100} height={100} />
            </Col>
        </Row>
        <Row className="justify-content-center">
            <Col>
                <Nav activeKey="/" className="justify-content-center">
                    <Nav.Link href="/">
                        <h1 className="text-center" style={{fontSize: "2rem"}}>Nereida</h1>
                    </Nav.Link>
                </Nav>
            </Col>
        </Row> 
    </Container>)
 
}



const Search = ({ fields}) => {
    const [res, setRes] = useState({data: null, error: null, isLoading: true})
    let editableQueryObj 
    if (typeof window !== "undefined"){
        const query = new URLSearchParams(window.location.search).get("query")
        let queryObj = JSON.parse(query)
        editableQueryObj = {...queryObj, timeRange: [...queryObj.timeRange].map(range => reformatRange(range) )}

        if (queryObj.subsequenceQuery){
            queryObj = {...queryObj,type: "complex"}
        }
        else if (queryObj.aggregationFilter && queryObj.aggregationFilter.length > 0){
            queryObj = {...queryObj,type: "aggregation"}
        }
        else{
            queryObj = {...queryObj,type: "simple"}
        }

        useEffect(() => useQuery(queryObj,setRes),[query]) //realizamos la consulta si esta ha cambiado
    }


    return(
    <>
        <Container fluid style={{maxWidth: "1000px"}} as="main">
            <SearchHeader  isLoading={res.isLoading}/>
            {editableQueryObj && <Accordion className="ml-4"  as="section" >
            <Row><p className="font-weight-bold">Consulta</p></Row>
                <AccordionSearch initSearch={editableQueryObj} fieldHelp={fields} index={1} save={true} />
            </Accordion>}
            {res.data !== null && 
            <SearchResults res={res}
                subLen={editableQueryObj.subsequenceQuery?editableQueryObj.subsequenceQuery.subsequence.length: 0} />}


        </Container>

    </>
    )

}

export default Search