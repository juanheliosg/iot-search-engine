import { Col, Row, Container, Pagination } from "react-bootstrap"
import ResultCard from "./ResultCard"
import { useState } from 'react'


const ErrorList = ({errors}) => {
    let errorsSet = new Set(errors)
    return(
        [...errorsSet].map( er => {
            console.error(er)
            return(
                <h5 className="ml-4 mt-4" >Error: {er.error} </h5>
            )
        })

    ) 
}

const resultsPerPage = 10
/**
 * Card component for showing a sensor result
 * @param {*} param0 
 * @returns 
 */
const ResultsList = ({res, subLen}) => {
    
    let [activePage, setPage] = useState(1)
    let pages = [<Pagination.Item key={1} onPointerDown={() => setPage(1)} active={1 === activePage}>
                    1
                </Pagination.Item>
    ]

    for (let i = 2; i <= Math.round(res.items/resultsPerPage); i++){
        pages.push(
            <Pagination.Item key={i} onPointerDown={() => {window.scrollTo(0, 0); setPage(i)}} active={i === activePage}>
                {i}
            </Pagination.Item>
        )
    }

    let series = [...res.series]
    if (subLen > 0 && series){
        series.sort((a,b) =>    Math.min(...(a.subsequences.map(el => el.ed))) - 
                                Math.min(...(b.subsequences.map( el => el.ed))
                                ))
    }
    

    if (res.series){
        series = series.slice(resultsPerPage*(activePage-1),resultsPerPage*(activePage-1)+resultsPerPage)
    }


    return(
    <>
        <Row className="ml-0 mt-4">
            <Col>
                <p className="font-weight-bold">Series recuperadas: {res.items}</p>
            </Col>
        </Row>
        { series && <Container as="section" fluid>
            {series.map(serie => 
                <ResultCard serie={serie} subLen={subLen}/>)}
        </Container>}
        {series && <Row className="justify-content-center mt-4">
                <Pagination>
                    {pages}
                </Pagination>
            </Row>}
    </>
    )

}

/**
 * Component for showing the results after a query
 * @param {} param0 
 * @returns 
 */
const SearchResults = ({res, subLen, page, resultsPerPage}) => {
    return(
    <>
        {res && res.error && 
            <ErrorList errors={res.data} />}
        {res && !res.error && !res.isLoading && 
            <ResultsList res={res.data} 
                        subLen={subLen} 
                        page={page} 
                        resultsPerPage={resultsPerPage} />}
    </>
    )
}

export default SearchResults