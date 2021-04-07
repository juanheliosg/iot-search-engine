# The IoT search engine
Repository for my bachelor's thesis. 

## How to run services
First you need to create extractor docker images locally using sbt extractor/docker:publishLocal. Then do docker-compose
up for starting the services.

For configuring druid kafka ingestion run python scripts/druid/postSpec.py. Now you are ready for start extracting data
