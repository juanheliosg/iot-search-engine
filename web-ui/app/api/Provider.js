


const getField = (field) => {
    return fetch(`${process.env.NEXT_PUBLIC_BASE_URL}/${field}`,{mode: "no-cors"})
        .then(res => res.json())
        .catch(data => console.log(data))

}

const postQuery = (searchQuery) => {
    return fetch(`${process.env.NEXT_PUBLIC_BASE_URL}/query`,{method: "POST", body:searchQuery})
        .then(res => res.json())

}

export{
    getField,
    postQuery
}