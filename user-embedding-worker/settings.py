from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    postgres_connect_hostname: str = "localhost"
    postgres_connect_port: int = 5430
    postgres_db: str = "coolture_db"
    postgres_user: str = "admin"
    postgres_password: str = "admin"

    embedding_model: str = "all-mpnet-base-v2"

    interval_minutes: int = 30
    weight_like: float = 1.0
    weight_dislike: float = -0.5
    weight_interested: float = 1.5
    weight_takes_part: float = 2.0


settings = Settings()
