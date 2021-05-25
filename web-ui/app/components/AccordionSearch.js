import { Accordion, Card,Row,Col, Button } from 'react-bootstrap'
import SearchSummary from './SearchSummary'
import AdvancedSearch from './AdvancedSearch'
import Link from 'next/link'
import { useAccordionToggle } from 'react-bootstrap/AccordionToggle';
import Search from './icons/Search'

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


const AccordionSearch = ({search,index}) =>{
    return(
    <Card as="article" style={{border: "0px"}}>
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
                <AdvancedSearch searchQuery={search} />
            </Card.Body>
        </Accordion.Collapse>
    </Card>
    )
}
export default AccordionSearch