docker-compose down -v
openapi-generator-cli generate -i ./querier/docs/querierAPIspec.yaml -g html2 -o querier/public/index.html
sbt querier/docker:publishLocal
sbt extractor/docker:publishLocal
docker build tsanalysis/ -t tsanalysis
rm -drf ./druid_data
mkdir ./druid_data
chmod 777 ./druid_data
openapi-generator-cli generate -i ./querier/docs/querierAPIspec.yaml -g html2 -o querier/public/
docker-compose up -d 

sleep 20
python ./scripts/druid/postSpec.py $1
