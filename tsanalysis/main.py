from fastapi import FastAPI
from models import SeriesQuery

app = FastAPI()

@app.post("/subsequence/search")
async def subsequenceSearch(query: SeriesQuery):
    return {"message": "He"}
