#!/usr/bin/env python3

import json
import os
import pika

RABBITMQ_HOST     = os.getenv("RABBITMQ_CONNECT_HOSTNAME", "localhost")
RABBITMQ_PORT     = int(os.getenv("RABBITMQ_CONNECT_PORT", "5672"))
RABBITMQ_USER     = os.getenv("RABBITMQ_USER", "admin")
RABBITMQ_PASSWORD = os.getenv("RABBITMQ_PASSWORD", "admin")
QUEUE             = "post.embedding.queue"
MAX_RETRIES       = 3

retry_counts: dict = {}


def on_message(channel, method, _properties, body):
    try:
        msg = json.loads(body)
        print(f"[embedding-worker] received post {msg['postId']}: {msg['title']!r} tags={msg.get('tags')}", flush=True)
        channel.basic_ack(delivery_tag=method.delivery_tag)
        retry_counts.pop(msg["postId"], None)
    except Exception as e:
        post_id = msg.get("postId", "?") if "msg" in dir() else "?"
        retries = retry_counts.get(post_id, 0) + 1
        retry_counts[post_id] = retries
        print(f"[embedding-worker] error on {post_id} (attempt {retries}/{MAX_RETRIES}): {e}", flush=True)
        requeue = retries < MAX_RETRIES
        if not requeue:
            retry_counts.pop(post_id, None)
        channel.basic_nack(delivery_tag=method.delivery_tag, requeue=requeue)


def main():
    credentials = pika.PlainCredentials(RABBITMQ_USER, RABBITMQ_PASSWORD)
    params = pika.ConnectionParameters(host=RABBITMQ_HOST, port=RABBITMQ_PORT, credentials=credentials,
                                       connection_attempts=10, retry_delay=3)
    connection = pika.BlockingConnection(params)
    channel = connection.channel()
    channel.queue_declare(queue=QUEUE, durable=True,
                          arguments={"x-dead-letter-exchange": "", "x-dead-letter-routing-key": "post.embedding.dlq"})
    channel.basic_qos(prefetch_count=1)
    channel.basic_consume(queue=QUEUE, on_message_callback=on_message)
    print(f"[embedding-worker] waiting for messages on {QUEUE}...", flush=True)
    channel.start_consuming()


if __name__ == "__main__":
    main()
