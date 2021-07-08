# The IoT search engine
Repository for my Bachelor's Thesis in the University of Granada. Feel free 

## Abstract
With the arrival of the Internet of Things, there is a growing number of devices
connected to the internet. The majority of these devices are sensors which are constantly
emitting their measures to the Internet. This massive amount of data can be used to have
a deeper understanding of the world in order to alter it. In this context the development of
search engines capable of search sensors by their contextual (location, type of measures...)
and content characteristics (by measurements values and by patterns of the sensors time
series) is vital for the evolution of the IoT. The problem of generic sensors searching in the
Internet is a Big Data problem. A massive volume of data with heterogeneous structure
at great speed with errors must be processed to accomplish this task. In this work the
state of art of IoT search engines is described, specially time series search and indexing
is focused. From this study a sensor engine capable of collecting a integrate public sensor
data is developed. This sensor engine can retrieve sensors by their contextual and content
attributes in a cloud environment. Also, a novel user interface is presented for introducing
time series for subsequence pattern search.

## How to run services
First you need to create service images extractor docker images. For Scala services (querier and extractor) use sbt 1.4.9 for building the services.

```sbt querier/docker:publishLocal```
```sbt extractor/docker:publishLocal```

For webui and tsanalysis services run docker build:

```docker build tsanalysis/ -t tsanalysis```
```docker build web-ui/ -t webui```

For druid be sure to create ```druid_data``` folder with correct permissions: ```chmod 777 ./druid_data```.

ENV variables in druidEnv, docker-compose.yml and web-ui/.env.local (you can find an example on web-ui/sample.env.local) must be changed for production deployment.

Finally run ``docker-compose up -d`` for running the containers.

For configuring druid kafka ingestion run ```python scripts/druid/postSpec.py```. Script for sample extractor can be run with ```python scripts/extractor/postSources.py```.

An automate script for doing this steps can be found in full_rebuild.sh. 