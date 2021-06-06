import { Accordion, Container } from 'react-bootstrap'
import Header from '../app/components/Header'
import getSearchExamples from '../app/examples/Searchs'
import AccordionSearch from '../app/components/AccordionSearch'
import GenericSearch from '../app/components/GenericSearch'
import reformatRange from '../app/utils/ReformatRange'



export default function Home({fields}) {
  
  const searchExamples = getSearchExamples()
  let searches = []
  if (typeof window !== "undefined"){ 
    let rawSearches = JSON.parse(localStorage.getItem("recentSearch"))
    
    if (rawSearches){
      console.log(rawSearches)
      searches = rawSearches.map(queryObj => 
        {return {...queryObj.search, timeRange: [...queryObj.search.timeRange].map(range => reformatRange(range) )}}
      )
    }

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
