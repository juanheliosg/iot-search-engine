mkdir ./druid_data
chmod 777 ./druid_data
docker-compose up -d 

sleep 10
python ./scripts/druid/postSpec.py $1
