import {getField, postQuery } from './provider'
var hash = require('object-hash');

const useField = (field) => {
    const getFieldList = (data) => {
        return data.map(el => el.name)
    }

    const {data, error} = useSWR(`/v1/${field}`,getField(field))
    return {
        fieldList = !error ?getFieldList(data) : data,
        isLoading: !error && !data,
        isError: error 
    }

}

const useFields = () => {

    const fieldNamesList = ["tags", "names", "cities", "regions", "countries", "sample_units", "measure", "measure_units"]
    const fieldList = fieldNamesList.map( name => useField(name))

    return fieldList
}

const postQuery = (query) => {
    //Hasheamos la query para hacer que la caché pueda funcionar bien
    const {data, error} = useSWR(`/v1/query/${hash(query)}`, postQuery(query))
    return {
        results = data,
        isLoading: !error && !data,
        isError: error 
    }

}

export { useFields, useQuery }