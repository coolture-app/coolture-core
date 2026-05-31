import json
import logging
import signal
import threading
from typing import Any, Callable

import pika
import psycopg
from psycopg_pool import ConnectionPool
from pika.adapters.blocking_connection import BlockingChannel, BlockingConnection
from pika.spec import Basic, BasicProperties
from sentence_transformers import SentenceTransformer
from tenacity import Retrying, stop_after_attempt, wait_exponential

from models import PostEmbeddingMessage, PostEmbeddingRecord
from settings import settings

logger = logging.getLogger("post-embedding-worker")


def _pika_params() -> pika.ConnectionParameters:
    return pika.ConnectionParameters(
        host=settings.rabbitmq_connect_hostname,
        port=settings.rabbitmq_connect_port,
        credentials=pika.PlainCredentials(settings.rabbitmq_user, settings.rabbitmq_password),
        connection_attempts=10,
        retry_delay=3,
    )


def _make_db_pool(minconn: int = 1, maxconn: int = 4) -> ConnectionPool:
    conninfo = (
        f"host={settings.postgres_connect_hostname} "
        f"port={settings.postgres_connect_port} "
        f"dbname={settings.postgres_db} "
        f"user={settings.postgres_user} "
        f"password={settings.postgres_password}"
    )
    return ConnectionPool(conninfo, min_size=minconn, max_size=maxconn)


def upsert_embedding(db_pool: ConnectionPool, record: PostEmbeddingRecord) -> None:
    with db_pool.connection() as conn:
        conn.execute(
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


def _processing_retry(max_attempts: int) -> Retrying:
    return Retrying(
        stop=stop_after_attempt(max_attempts),
        wait=wait_exponential(multiplier=0.5, max=30),
        reraise=True,
    )


def _make_on_message(
    model: SentenceTransformer,
    db_pool: ConnectionPool,
    max_retries: int,
) -> Callable:
    def on_message(
        channel: BlockingChannel,
        method: Basic.Deliver,
        _properties: BasicProperties,
        body: bytes,
    ) -> None:
        msg: PostEmbeddingMessage | None = None
        try:
            msg = PostEmbeddingMessage.model_validate(json.loads(body))
            logger.info("processing post %s: %r", msg.post_id, msg.title)

            for attempt in _processing_retry(max_retries):
                with attempt:
                    vector = model.encode(msg.text).tolist()
                    upsert_embedding(
                        db_pool,
                        PostEmbeddingRecord(post_id=msg.post_id, embedding=vector, model=settings.embedding_model),
                    )

            channel.basic_ack(delivery_tag=method.delivery_tag)
            logger.info("stored embedding for post %s", msg.post_id)

        except Exception as e:
            post_id = str(msg.post_id) if msg else "?"
            logger.warning("error on %s after %d attempts, sending to DLQ: %s", post_id, max_retries, e)
            channel.basic_nack(delivery_tag=method.delivery_tag, requeue=False)

    return on_message


def _make_on_dlq_message(
    model: SentenceTransformer,
    db_pool: ConnectionPool,
    max_retries: int,
) -> Callable:
    def on_dlq_message(
        channel: BlockingChannel,
        method: Basic.Deliver,
        _properties: BasicProperties,
        body: bytes,
    ) -> None:
        msg: PostEmbeddingMessage | None = None
        try:
            msg = PostEmbeddingMessage.model_validate(json.loads(body))
            logger.warning("DLQ processing post %s: %r", msg.post_id, msg.title)

            for attempt in _processing_retry(max_retries + 2):
                with attempt:
                    vector = model.encode(msg.text).tolist()
                    upsert_embedding(
                        db_pool,
                        PostEmbeddingRecord(post_id=msg.post_id, embedding=vector, model=settings.embedding_model),
                    )

            channel.basic_ack(delivery_tag=method.delivery_tag)
            logger.info("DLQ recovered embedding for post %s", msg.post_id)

        except Exception:
            post_id = str(msg.post_id) if msg else "?"
            logger.critical("post %s permanently failed, dropping", post_id)
            channel.basic_ack(delivery_tag=method.delivery_tag)

    return on_dlq_message


def _declare_infra(channel: BlockingChannel) -> None:
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


def run_consumer(
    queue: str,
    callback: Callable,
    should_stop: threading.Event,
    setup: Callable[[BlockingChannel], None] | None = None,
    stop_channel_ref: list[BlockingChannel] | None = None,
) -> None:
    while not should_stop.is_set():
        try:
            connection = BlockingConnection(_pika_params())
            channel = connection.channel()
            if setup:
                setup(channel)
            channel.basic_qos(prefetch_count=1)
            channel.basic_consume(queue=queue, on_message_callback=callback)
            if stop_channel_ref is not None:
                stop_channel_ref.clear()
                stop_channel_ref.append(channel)
            logger.info("waiting on %s...", queue)
            channel.start_consuming()
            if stop_channel_ref is not None:
                stop_channel_ref.clear()

        except pika.exceptions.ConnectionClosedByBroker:
            logger.warning("[%s] connection closed by broker, stopping", queue)
            break
        except pika.exceptions.AMQPChannelError as e:
            logger.warning("[%s] channel error: %s, stopping", queue, e)
            break
        except pika.exceptions.AMQPConnectionError as e:
            if should_stop.is_set():
                break
            logger.warning("[%s] connection error: %s, reconnecting in 3s", queue, e)
            should_stop.wait(3)
        except Exception as e:
            if should_stop.is_set():
                break
            logger.warning("[%s] unexpected error: %s, reconnecting in 3s", queue, e)
            should_stop.wait(3)


def _handle_signal(
    signum: int,
    _frame: Any,
    should_stop: threading.Event,
    main_channel_ref: list[BlockingChannel],
) -> None:
    logger.info("received %s, shutting down...", signal.Signals(signum).name)
    should_stop.set()
    if main_channel_ref:
        main_channel_ref[0].stop_consuming()


def main() -> None:
    logging.basicConfig(level=logging.INFO, format="[%(name)s] %(levelname)s %(message)s")
    logger.info("starting post-embedding-worker (model=%s)", settings.embedding_model)

    model = SentenceTransformer(settings.embedding_model)
    db_pool = _make_db_pool()

    should_stop = threading.Event()
    main_channel_ref: list[BlockingChannel] = []

    signal.signal(
        signal.SIGTERM,
        lambda s, f: _handle_signal(s, f, should_stop, main_channel_ref),
    )
    signal.signal(
        signal.SIGINT,
        lambda s, f: _handle_signal(s, f, should_stop, main_channel_ref),
    )

    on_message = _make_on_message(model, db_pool, settings.embedding_max_retries)
    on_dlq_message = _make_on_dlq_message(model, db_pool, settings.embedding_max_retries)

    dlq_thread = threading.Thread(
        target=run_consumer,
        kwargs={
            "queue": settings.embedding_dlq,
            "callback": on_dlq_message,
            "should_stop": should_stop,
            "setup": lambda ch: ch.queue_declare(queue=settings.embedding_dlq, durable=True),
        },
        name="dlq-consumer",
    )
    dlq_thread.start()

    run_consumer(
        queue=settings.embedding_queue,
        callback=on_message,
        should_stop=should_stop,
        setup=_declare_infra,
        stop_channel_ref=main_channel_ref,
    )

    logger.info("waiting for DLQ consumer to finish...")
    dlq_thread.join(timeout=5)
    db_pool.close()
    logger.info("shutdown complete")


if __name__ == "__main__":
    main()
