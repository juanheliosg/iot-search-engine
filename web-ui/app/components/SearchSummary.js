
const getTranslatedFilter = (aggFilter) => {
    switch(aggFilter){
        case "avg":
            return "media aritmética"
        case "std":
            return "desviación estándar"
        case "sum":
            return "suma"
        case "max":
            return "máximo"
        case "min":
            return "mínimo"
        case "count":
            return "número"
    }
}



const getSearchSummaryText = (search) => {
    let formattedFilter = search.filter
        .replace("measure_name","medicion")
        .replace("measure","valor")
        .replace("description","descripcion")
        .replace("unit","unidad")
        .replace("measure_desc","descripción de la medida")
        .replace("country","pais")
        .replace("region","region")
        .replace("city","ciudad")
        .replace("address","dirección postal")
        .replace("name","nombre de la fuente")
        .replace("lat","latitud")
        .replace("long","longitud")
        .replace("AND" , "y")
        .replace(":","=")
        .replace("OR", "o")
        .replace("LIKE", "parecidos a")

       

    let aggregationSummary = search.aggregationFilter?
        search.aggregationFilter.map( agg => {
            let formattedOperation = getTranslatedFilter(agg.operation)
            let formattedComparation = agg.aggComparation? getTranslatedFilter(agg.aggComparation) : ""
            return `${formattedOperation} ${agg.relation? agg.relation.replace(":","=") : ""} ${formattedComparation} ${agg.value? agg.value: ""}`
        }).join(" y ") : ""
    

    let timeRangeSummary = 
        search.timeRange.map( range => {
            return `desde el ${range.lowerBound.toLocaleString()} al  ${range.upperBound.toLocaleString()}`
        }).join(" y ")
    
    let summary = `Sensores ${timeRangeSummary} tal que ${formattedFilter} 
        ${aggregationSummary.length > 0? `y con ${aggregationSummary}` : ""} 
        ${search.subsequenceQuery? " con búsqueda por subsecuencia " : ""}
        ${search.timeseries == true? "devolviendo la serie temporal" : " sin serie"}`
    

    return summary
}

const SearchSummary = ({search}) => {
    let textSummary = getSearchSummaryText(search)
    
    return(
        <p className="pb-0 mb-0">{textSummary}</p>
    )

}

export default SearchSummary