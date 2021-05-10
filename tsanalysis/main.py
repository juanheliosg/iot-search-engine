from fastapi import FastAPI
from models import SeriesQuery
from settings import Settings
from pyspark.sql import SparkSession

app = FastAPI()
settings = Settings()
spark: SparkSession = SparkSession.builder\
    .master(settings.spark_master_url)\
    .appName("tseriesanalysis").getOrCreate()

@app.post("/subsequence/search")
async def subsequenceSearch(query: SeriesQuery):
    return {"message": "He"}
