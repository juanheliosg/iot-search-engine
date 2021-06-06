import { Accordion, Card,Row,Col, Button, Form } from 'react-bootstrap'
import SearchSummary from './SearchSummary'
import AdvancedSearch from './AdvancedSearch'
import { useState} from 'react'
import { useAccordionToggle } from 'react-bootstrap/AccordionToggle';
import Search from './icons/Search'
import Link from 'next/link'

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
    const [validated, setValidated] = useState(false);

    const handleSubmit = (event) => {
        const form = event.currentTarget;
        if (form.checkValidity() === false) {
            event.preventDefault();
            event.stopPropagation();
        }
        else if (searchQuery.subsequenceQuery){
              if (searchQuery.subsequenceQuery.subsequence.length < 3){
                  event.preventDefault();
                  event.stopPropagation();
              }
          }
        
        setValidated(true);
      };
   
    return(
    <Card key={index} as="article" style={{border: "0px", overflow: "visible"}}>
        <Card.Header className="pt-1 pb-0" style={{backgroundColor: "rgba(0,0,0,0)"}}>
            <Row>
                <Col xs={10} className="mr-0 pr-0">
                    <SearchSummary search={search} />
                </Col>
                <Col xs={2} className="ml-0 pr-0">
                    <Link href={{pathname: "/search", query: { query: JSON.stringify(search)}}}>
                        <Button variant="link">
                            <Search />
                        </Button>
                    </Link>
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
            <Form noValidate validated={validated} onSubmit={handleSubmit}>
                <AdvancedSearch ind={index} searchQuery={search} setSearch={setSearch} fieldHelp={fieldHelp} />
                <Form.Row className="justify-content-center mt-1">
                    <Link href={{pathname: "/search", query: { query: JSON.stringify(search)}}}>
                        <Button type="submit" variant="link">
                        Buscar
                       </Button>
                    </Link>
                </Form.Row>
            </Form>
            </Card.Body>

        </Accordion.Collapse>
    </Card>
    )
}
export default AccordionSearch