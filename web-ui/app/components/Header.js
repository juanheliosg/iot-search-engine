import Image from "next/image"
import { Container,Row,Col } from "react-bootstrap";

const Header = () => {
    return(
        <Container as="header">
            <Row className="justify-content-center">
                <Col md="auto" className="align-items-center">
                    <Image src="/logo.svg" width={120} height={120}/>          
                </Col>
            </Row>
            <Row className="justify-content-center">
                <Col>
                    <h1 className="text-center">Nereida</h1>
                </Col>
            </Row>
            <Row className="justify-content-center">
                <Col>
                <p className="text-center font-weight-light">Un buscador para el internet de las cosas</p>
                </Col>
            </Row>
        </Container>
    )
}

export default Header