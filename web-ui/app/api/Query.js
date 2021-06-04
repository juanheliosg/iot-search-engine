import useSWR from 'swr'
const fetcher = url => fetch(url).then(res=> res.json())

const useField = (field) => {
    const getFieldList = (data) => {
        console.log(data)
        return data.count.map(el => el.name)
    }

    const {data, error} = useSWR(`${process.env.NEXT_PUBLIC_BASE_URL}/${field}`, fetcher)
    
    return {
        fieldList: !error && data ?getFieldList(data) : data,
        isLoading: !error && !data,
        isError: error 
    }

}

const useFields = () => {
    const fieldNamesList = ["tags", "names", "cities", "regions", "countries", "sample_units", "measure", "measure_units"]
    const fieldList = fieldNamesList.map( name => { return [name, useField(name)] })
    const fieldObject =  Object.assign(...fieldList.map(([key, val]) => ({[key]: val})))
    return fieldObject
}

/**
 * const useQuery = (query) => {
    //Hasheamos la query para hacer que la caché pueda funcionar bien
    //
    const {data, error} = useSWR(`/v1/query/${hash(query)}`, postQuery(query))
    return {
        results: data,
        isLoading: !error && !data,
        isError: error 
    }

}
*/
const useQuery = (query) =>{
    return "a"
}

export { useFields , useQuery }