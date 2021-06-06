import useSWR from 'swr'
import axios from 'axios'
import {useState} from 'react'
import {useCallback} from 'react'


const fetcher = url => fetch(url).then(res=> res.json())

const useField = (field) => {
    const getFieldList = (data) => {
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
 * Post hook
 * @param query to be send to the url
 * @returns 
 */

const useQuery = (payload, setRes) => {
    setRes({data: null, error: null, isLoading: true})
    
    axios.post(`${process.env.NEXT_PUBLIC_BASE_URL}/query`,payload).then(res => {
            setRes({data: res.data, isLoading: false, error: null});
         }).catch((error) => {
            setRes({data: error.response.data, isLoading: false, error});
         })
}


export { useFields , useQuery }