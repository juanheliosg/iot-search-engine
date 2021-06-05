import {  Container,Row,Col } from 'react-bootstrap'
import Image from 'next/image'
import Link from 'next/link'

const SearchHeader = () => {
    return(
        
            <Container className="mt-5" fluid style={{maxWidth: "800px"}} as="main">
                <Row className="align-items-center">
                    <Col xs={2} className="pr-0">
                    <Link href="/">
                        <Image src="/logo.svg" width={100} height={100}  />
                    </Link> 
                    </Col>
                    <Col xs={2}>
                        <h1 className="text-center" style={{fontSize: "2rem"}}>Nereida</h1>
                    </Col>
                </Row>
            </Container>


    )
}

export default SearchHeader