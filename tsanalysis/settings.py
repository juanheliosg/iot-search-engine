from pydantic import BaseSettings

class Settings(BaseSettings):
    spark_master_url: str ="local[1]"
