CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS post_embeddings
(
    post_id UUID NOT NULL
        CONSTRAINT pk_post_embeddings PRIMARY KEY
        CONSTRAINT fk_post_embeddings_post_id
            REFERENCES posts (id)
            ON DELETE CASCADE,

    embedding  vector(1536) NOT NULL,
    model      VARCHAR(128) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_post_embeddings_hnsw
    ON post_embeddings
    USING hnsw (embedding vector_cosine_ops);


CREATE TABLE IF NOT EXISTS user_embeddings
(
    user_id UUID NOT NULL
        CONSTRAINT pk_user_embeddings PRIMARY KEY
        CONSTRAINT fk_user_embeddings_user_id
            REFERENCES users (id)
            ON DELETE CASCADE,

    embedding  vector(1536) NOT NULL,
    model      VARCHAR(128) NOT NULL,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_user_embeddings_hnsw
    ON user_embeddings
    USING hnsw (embedding vector_cosine_ops);
