import logging
import signal
import threading
import time
from collections import defaultdict
from typing import Any

import numpy as np
import psycopg

from settings import settings

logger = logging.getLogger("user-embedding-worker")

WEIGHTS: dict[str, float] = {
    "LIKE": settings.weight_like,
    "DISLIKE": settings.weight_dislike,
    "INTERESTED": settings.weight_interested,
    "TAKES_PART": settings.weight_takes_part,
}


def _make_db_pool() -> psycopg.ConnectionPool:
    conninfo = (
        f"host={settings.postgres_connect_hostname} "
        f"port={settings.postgres_connect_port} "
        f"dbname={settings.postgres_db} "
        f"user={settings.postgres_user} "
        f"password={settings.postgres_password}"
    )
    return psycopg.ConnectionPool(conninfo, min_size=1, max_size=2)


def run_once(db_pool: psycopg.ConnectionPool) -> int:
    user_data: dict[str, list[tuple[np.ndarray, float]]] = defaultdict(list)
    upserted = 0

    with db_pool.connection() as conn:
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
                if weight != 0.0:
                    user_data[user_id].append((np.array(embedding, dtype=np.float32), weight))

        if not user_data:
            logger.info("no interactions with embeddings found, skipping")
            return 0

        with conn.cursor() as cur:
            for user_id, weighted_vecs in user_data.items():
                vectors = np.array([v for v, _ in weighted_vecs])
                weights = np.array([w for _, w in weighted_vecs])

                total_weight = weights.sum()
                if total_weight == 0:
                    continue

                vector = (vectors * weights[:, np.newaxis]).sum(axis=0) / total_weight
                norm = np.linalg.norm(vector)
                if norm > 1e-9:
                    vector /= norm

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

    logger.info("upserted user embeddings for %d users", upserted)
    return upserted


def main() -> None:
    logging.basicConfig(level=logging.INFO, format="[%(name)s] %(levelname)s %(message)s")
    logger.info(
        "starting user-embedding-worker (interval=%d min, weights=%s)",
        settings.interval_minutes,
        WEIGHTS,
    )

    db_pool = _make_db_pool()
    should_stop = threading.Event()

    def _handle_signal(signum: int, _frame: Any) -> None:
        logger.info("received %s, shutting down...", signal.Signals(signum).name)
        should_stop.set()

    signal.signal(signal.SIGTERM, _handle_signal)
    signal.signal(signal.SIGINT, _handle_signal)

    interval = settings.interval_minutes * 60

    while not should_stop.is_set():
        run_start = time.monotonic()
        try:
            run_once(db_pool)
        except Exception as e:
            logger.error("run failed: %s", e, exc_info=True)

        elapsed = time.monotonic() - run_start
        sleep_time = max(interval - elapsed, 5)
        logger.debug("sleeping for %.1f seconds", sleep_time)
        should_stop.wait(timeout=sleep_time)

    db_pool.close()
    logger.info("shutdown complete")


if __name__ == "__main__":
    main()
