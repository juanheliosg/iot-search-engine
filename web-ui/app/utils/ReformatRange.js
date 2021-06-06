


/**
 * Transform a range from epochSeconds or string to JS date if is not in date string format
 * @param {range} range  
 */
const reformatRange = (range) => {
    if (range['lowerBound'] instanceof Date && range['upperBound'] instanceof Date ){
        return range
    }
    else{
        let lowerBound = Date.parse(range['lowerBound'])
        let upperBound =  Date.parse(range['upperBound'])
        //por algun motivo JS parse los Date strings a second epoch
        //Por eso hay que hacer de nuevo la conversión. 
        if (typeof lowerBound == "number"){
            lowerBound = new Date(range['lowerBound'])
        }
        if (typeof upperBound == "number"){
            upperBound = new Date(range['upperBound'])
        }
        
        return {lowerBound: lowerBound, 
            upperBound: upperBound
        }
    }
}

export default reformatRange