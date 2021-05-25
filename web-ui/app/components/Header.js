import Image from "next/image"
import { Container,Row,Col } from "react-bootstrap";

const Header = () => {
    return(
        <Container as="header" className="mb-3" style = {{maxWidth: "800px"}}>
            <Row className="justify-content-center">
                <Col md="auto" className="align-items-center mt-3">
                    <Image src="/logo.svg" width={100} height={100}/>          
                </Col>
            </Row>
            <Row className="justify-content-center">
                <Col>
                    <h1 className="text-center" style={{fontSize: "2rem"}}>Nereida</h1>
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