# post-embedding-worker

Listens to a RabbitMQ queue for new/updated posts, generates their vector embeddings using a sentence-transformer model, and stores them in PostgreSQL.

## How to run

```bash
uv run python main.py
```

## uv.lock

Auto-generated lockfile that pins every dependency (including transitive ones) to exact versions so builds are reproducible. Don't edit it manually — `uv sync` updates it.

## Dependencies

| Package | Why |
|---|---|
| `pika` | RabbitMQ client |
| `sentence-transformers` | NLP model for text embeddings |
| `psycopg[binary]` | PostgreSQL driver |
| `psycopg-pool` | Connection pooling |
| `pgvector` | Vector similarity search in Postgres |
| `pydantic` | Message/record models |
| `pydantic-settings` | Config from env vars |
| `tenacity` | Retry logic with backoff |
