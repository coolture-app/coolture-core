import time
from collections import defaultdict

import numpy as np
import psycopg2
from pgvector.psycopg2 import register_vector

from settings import settings

WEIGHTS: dict[str, float] = {
    "LIKE":       settings.weight_like,
    "DISLIKE":    settings.weight_dislike,
    "INTERESTED": settings.weight_interested,
    "TAKES_PART": settings.weight_takes_part,
}


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


def run_once() -> None:
    print("[user-embedding-worker] starting user embedding update run...", flush=True)

    user_data: dict = defaultdict(list)

    with get_db() as conn:
        with conn.cursor() as cur:
            cur.execute(
                """
                SELECT interactions.user_id, pe.embedding, interactions.type
                FROM (
                    SELECT user_id, post_id, type FROM post_reactions
                    UNION ALL
                    SELECT user_id, post_id, type FROM post_participations
                ) interactions
                JOIN post_embeddings pe ON pe.post_id = interactions.post_id
                """
            )
            for user_id, embedding, interaction_type in cur.fetchall():
                weight = WEIGHTS.get(interaction_type, 0.0)
                user_data[user_id].append((np.array(embedding, dtype=np.float32), weight))

    if not user_data:
        print("[user-embedding-worker] no interactions with embeddings found, skipping.", flush=True)
        return

    upserted = 0
    with get_db() as conn:
        with conn.cursor() as cur:
            for user_id, weighted_vecs in user_data.items():
                vectors = np.array([v for v, _ in weighted_vecs])
                weights = np.array([w for _, w in weighted_vecs])

                total_weight = weights.sum()
                if total_weight == 0:
                    continue

                vector = (vectors * weights[:, np.newaxis]).sum(axis=0) / total_weight
                vector /= np.linalg.norm(vector) + 1e-9

                cur.execute(
                    """
                    INSERT INTO user_embeddings (user_id, embedding, model, updated_at)
                    VALUES (%s, %s, %s, now())
                    ON CONFLICT (user_id) DO UPDATE
                        SET embedding  = EXCLUDED.embedding,
                            model      = EXCLUDED.model,
                            updated_at = now()
                    """,
                    (str(user_id), vector.tolist(), settings.embedding_model),
                )
                upserted += 1

    print(f"[user-embedding-worker] upserted user embeddings for {upserted} users.", flush=True)


def main() -> None:
    interval = settings.interval_minutes * 60
    print(f"[user-embedding-worker] running every {settings.interval_minutes} minutes.", flush=True)
    while True:
        try:
            run_once()
        except Exception as e:
            print(f"[user-embedding-worker] run failed: {e}", flush=True)
        time.sleep(interval)


if __name__ == "__main__":
    main()
