import { Accordion, Card,Row,Col, Button, Form } from 'react-bootstrap'
import SearchSummary from './SearchSummary'
import AdvancedSearch from './AdvancedSearch'
import { useState} from 'react'
import { useAccordionToggle } from 'react-bootstrap/AccordionToggle';
import Search from './icons/Search'

/**
 * Custom toggle button for displaying the advanced search
 * @param children children props for customization
 * @param eventKey event key
 * @returns 
 */
const ToggleButton = ({children, eventKey}) => {
    const decoratedOnClick = useAccordionToggle(eventKey);

  return (
    <button
      type="button"
      style={{ backgroundColor:  "rgba(0,0,0,0)", border: "0px"}}
      onClick={decoratedOnClick}
    >
      {children}
    </button>
  );
}


/**
 * Query accordion including a summary in a human readable format and 
 * a form for updating the query.
 * @param {initSearch}
 * @param {index} 
 * @returns 
 */
const AccordionSearch = ({initSearch,index, fieldHelp}) =>{
    const [search, setSearch] = useState(initSearch)
    
    

    return(
    <Card key={index} as="article" style={{border: "0px", overflow: "visible"}}>
        <Card.Header className="pt-1 pb-0" style={{backgroundColor: "rgba(0,0,0,0)"}}>
            <Row>
                <Col xs={10} className="mr-0 pr-0">
                    <SearchSummary search={search} />
                </Col>
                <Col xs={2} className="ml-0 pr-0">
                    <Button variant="link" href="/results">
                        <Search />
                    </Button>
                </Col>
            </Row>
            <Row className="justify-content-end">
                <Col xs={2} className="ml-0 pr-0" >
                    <ToggleButton eventKey={index.toString()}>
                        <p className="text-primary pb-0">Editar</p>
                    </ToggleButton>
                </Col>
            </Row>
        </Card.Header>
        <Accordion.Collapse eventKey={index.toString()}>
            <Card.Body>
                <AdvancedSearch ind={index} searchQuery={search} setSearch={setSearch} fieldHelp={fieldHelp} />
                <Form.Row className="justify-content-center mt-1">
                <Button variant="link">
                    Buscar
                </Button>
            </Form.Row>
            </Card.Body>

        </Accordion.Collapse>
    </Card>
    )
}
export default AccordionSearch