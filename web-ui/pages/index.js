import { Accordion, AccordionCollapse, Card, Container,Row,Col, Button } from 'react-bootstrap'
import Header from '../app/components/Header'
import getSearchExamples from '../app/examples/Searchs'
import AccordionSearch from '../app/components/AccordionSearch'


export default function Home() {
  let searchExamples = getSearchExamples()
  var searches = []
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
          <Accordion as="section">
            {searches.map( (search,index) => {
            return(
              <AccordionSearch search={search} index={index} />
            )
          })}
          </Accordion>
      </Container>
    </>
  )
}
