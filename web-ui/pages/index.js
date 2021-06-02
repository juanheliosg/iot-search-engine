import { Accordion, AccordionCollapse, Card, Container,Row,Col, Button } from 'react-bootstrap'
import Header from '../app/components/Header'
import getSearchExamples from '../app/examples/Searchs'
import AccordionSearch from '../app/components/AccordionSearch'
import GenericSearch from '../app/components/GenericSearch'

export default function Home() {
  const searchExamples = getSearchExamples()
  let searches = []
  if (typeof window !== "undefined"){
    searches = JSON.parse(localStorage.getItem("recentSearches"))
  }

  if (!searches){
    searches = searchExamples
  }
  else if ( searches && searches.length < 3) {
    searches = searches.concat(searchExamples.slice(0,searchExamples.length - searches.length))
  }

  return (
    <>
      <Header />
      <Container fluid style={{maxWidth: "800px"}} as="main">
        <Container as="section">
          <h2 style={{fontSize: "1rem"}}>Haz tu propia consulta</h2>
          <GenericSearch />
        </Container>
        <h2 style={{fontSize: "1rem"}}>Consultas previas</h2>
          <Accordion as="section" >
            {searches.map( (search,index) => {
            return(
              <AccordionSearch initSearch={search} index={index} />
            )
          })}
          </Accordion>
      </Container>
    </>
  )
}
