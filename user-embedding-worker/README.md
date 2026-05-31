# user-embedding-worker

Computes user embeddings from their post interactions (likes, dislikes, participations) every N minutes.

## How to run

```bash
uv run python main.py
```

## uv.lock

Auto-generated lockfile that pins every dependency (including transitive ones) to exact versions so builds are reproducible. Don't edit it manually — `uv sync` updates it.

## Dependencies

| Package | Why |
|---|---|
| `psycopg[binary]` | PostgreSQL driver |
| `psycopg-pool` | Connection pooling |
| `pgvector` | Vector similarity search in Postgres |
| `numpy` | Vector math |
| `pydantic-settings` | Config from env vars |
