from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    rabbitmq_connect_hostname: str = "localhost"
    rabbitmq_connect_port: int = 5672
    rabbitmq_user: str = "admin"
    rabbitmq_password: str = "admin"

    postgres_connect_hostname: str = "localhost"
    postgres_connect_port: int = 5430
    postgres_db: str = "coolture_db"
    postgres_user: str = "admin"
    postgres_password: str = "admin"

    embedding_queue: str = "post.embedding.queue"
    embedding_dlq: str = "post.embedding.dlq"
    embedding_exchange: str = "embedding.exchange"
    embedding_routing_key: str = "post.embedding"
    embedding_model: str = "all-mpnet-base-v2"
    embedding_max_retries: int = 3


settings = Settings()
