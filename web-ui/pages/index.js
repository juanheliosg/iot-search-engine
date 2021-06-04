import { Accordion, AccordionCollapse, Card, Container,Row,Col, Button } from 'react-bootstrap'
import Header from '../app/components/Header'
import getSearchExamples from '../app/examples/Searchs'
import AccordionSearch from '../app/components/AccordionSearch'
import GenericSearch from '../app/components/GenericSearch'
import { useFields } from '../app/api/Query'



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
  const fields = useFields()

  return (
    <>
    <h1></h1>
      <Header />
      <Container fluid style={{maxWidth: "800px"}} as="main">
        <Container as="section">
          <h2 style={{fontSize: "1rem"}}>Haz tu propia consulta</h2>
          <GenericSearch fieldHelp={{fields}} />
        </Container>
        <h2 style={{fontSize: "1rem"}}>Consultas previas</h2>
          <Accordion as="section" >
            {searches.map( (search,index) => {
            return(
              <AccordionSearch fieldHelp={fields} initSearch={search} index={index} key={index}/>
            )
          })}
          </Accordion>
          
      </Container>
    </>
  )
}
