import axios from 'axios'

import { handleResponse, handleError } from './response'

const BASE_URL = process.env.BASE_URL

const getField = (field) => {
    return axios
        .get(`${BASE_URL}/${field}`)
        .then(res => res.data)  
}

const postQuery = (searchQuery) => {
    return axios
        .post(`${BASE_URL}/query`,searchQuery)
        .then(res => res.data)

}

export const apiProvider = {
    getField,
    postQuery
}