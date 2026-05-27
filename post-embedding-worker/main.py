import json
import threading

import pika
import psycopg2
from pgvector.psycopg2 import register_vector
from sentence_transformers import SentenceTransformer

from models import PostEmbeddingMessage, PostEmbeddingRecord
from settings import settings

model = SentenceTransformer(settings.embedding_model)


def _pika_params() -> pika.ConnectionParameters:
    return pika.ConnectionParameters(
        host=settings.rabbitmq_connect_hostname,
        port=settings.rabbitmq_connect_port,
        credentials=pika.PlainCredentials(settings.rabbitmq_user, settings.rabbitmq_password),
        connection_attempts=10,
        retry_delay=3,
    )


def get_db() -> psycopg2.extensions.connection:
    conn = psycopg2.connect(
        host=settings.postgres_connect_hostname,
        port=settings.postgres_connect_port,
        dbname=settings.postgres_db,
        user=settings.postgres_user,
        password=settings.postgres_password,
    )
    register_vector(conn)
    return conn


def upsert_embedding(record: PostEmbeddingRecord) -> None:
    with get_db() as conn:
        with conn.cursor() as cur:
            cur.execute(
                """
                INSERT INTO post_embeddings (post_id, embedding, model)
                VALUES (%s, %s, %s)
                ON CONFLICT (post_id) DO UPDATE
                    SET embedding  = EXCLUDED.embedding,
                        model      = EXCLUDED.model,
                        updated_at = now()
                """,
                (str(record.post_id), record.embedding, record.model),
            )


def _death_count(properties: pika.BasicProperties) -> int:
    headers = properties.headers or {}
    x_death = headers.get("x-death", [])
    return sum(entry.get("count", 0) for entry in x_death)


def on_message(channel, method, properties, body) -> None:
    msg: PostEmbeddingMessage | None = None
    try:
        msg = PostEmbeddingMessage.model_validate(json.loads(body))
        print(f"[post-embedding-worker] processing post {msg.postId}: {msg.title!r}", flush=True)

        vector = model.encode(msg.text).tolist()
        record = PostEmbeddingRecord(post_id=msg.postId, embedding=vector, model=settings.embedding_model)
        upsert_embedding(record)

        channel.basic_ack(delivery_tag=method.delivery_tag)
        print(f"[post-embedding-worker] stored embedding for post {msg.postId}", flush=True)

    except Exception as e:
        post_id = str(msg.postId) if msg else "?"
        attempts = _death_count(properties) + 1
        print(
            f"[post-embedding-worker] error on {post_id} "
            f"(attempt {attempts}/{settings.embedding_max_retries}): {e}",
            flush=True,
        )
        requeue = attempts < settings.embedding_max_retries
        channel.basic_nack(delivery_tag=method.delivery_tag, requeue=requeue)


def on_dlq_message(channel, method, _properties, body) -> None:
    try:
        data = json.loads(body)
        post_id = data.get("postId", "?")
        print(f"[post-embedding-worker][DLQ] permanently failed post {post_id}: {data}", flush=True)
    except Exception as e:
        print(f"[post-embedding-worker][DLQ] could not parse message: {e} | raw: {body}", flush=True)
    finally:
        channel.basic_ack(delivery_tag=method.delivery_tag)


def run_dlq_consumer() -> None:
    connection = pika.BlockingConnection(_pika_params())
    channel = connection.channel()
    channel.queue_declare(queue=settings.embedding_dlq, durable=True)
    channel.basic_qos(prefetch_count=1)
    channel.basic_consume(queue=settings.embedding_dlq, on_message_callback=on_dlq_message)
    print(f"[post-embedding-worker][DLQ] waiting on {settings.embedding_dlq}...", flush=True)
    channel.start_consuming()


def main() -> None:
    dlq_thread = threading.Thread(target=run_dlq_consumer, daemon=True, name="dlq-consumer")
    dlq_thread.start()

    connection = pika.BlockingConnection(_pika_params())
    channel = connection.channel()
    channel.exchange_declare(exchange=settings.embedding_exchange, exchange_type="direct", durable=True)
    channel.queue_declare(
        queue=settings.embedding_queue,
        durable=True,
        arguments={
            "x-dead-letter-exchange": "",
            "x-dead-letter-routing-key": settings.embedding_dlq,
        },
    )
    channel.queue_bind(
        queue=settings.embedding_queue,
        exchange=settings.embedding_exchange,
        routing_key=settings.embedding_routing_key,
    )
    channel.basic_qos(prefetch_count=1)
    channel.basic_consume(queue=settings.embedding_queue, on_message_callback=on_message)
    print(f"[post-embedding-worker] waiting on {settings.embedding_queue}...", flush=True)
    channel.start_consuming()


if __name__ == "__main__":
    main()
