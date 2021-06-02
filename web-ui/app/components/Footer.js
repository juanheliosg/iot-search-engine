import { Container,Row,Col } from "react-bootstrap";
import Wave from "../../public/wave.svg"
import Github from "./icons/Github";
import Mailbox from "./icons/Mailbox"


const Footer = () => {
    return(
    
            <Container as="footer" className="d-flex justify-content-center align-self-end" fluid={true} style={{height: '300px', backgroundImage: `url(/wave.svg)`}}>
                <Row as="section" className="align-self-end">
                    <Col xs lg="2">
                    <a href="https://github.com/juanheliosg">
                        <Github />
                    </a>
                    </Col>
                    <Col md="auto">
                        <p>Nereida 2020-2021</p>
                    </Col>
                    <Col xs lg="2">
                        <a href="mailto:juanheliosg@correo.ugr.es">
                                    <Mailbox />
                        </a>
                    </Col>     
                </Row>
            </Container>
      

    )
}
export default Footer