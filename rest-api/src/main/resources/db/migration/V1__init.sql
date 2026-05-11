-- INITIAL DB SCHEMA FOR THE COOLTURE
-- Created from pg_dump then edited to be easier to maintain and understand
-- FOR MORE DETAILS LOOK AT SCHEMA.DBML AND DBDOCS.IO OF THE PROJECT
-- CREATE TABLES FROM THE MOST INDEPENDENT ONES
CREATE TABLE country_codes
(
    code VARCHAR(3)
        CONSTRAINT pk_country_codes PRIMARY KEY
);

CREATE TABLE event_categories
(
    id UUID
        CONSTRAINT pk_event_categories PRIMARY KEY,
    name VARCHAR(32)
        NOT NULL
        CONSTRAINT uq_event_categories_name UNIQUE
);

CREATE TABLE users
(
    id UUID
        CONSTRAINT pk_users PRIMARY KEY,
    username VARCHAR(32)
        NOT NULL
        CONSTRAINT uq_users_username UNIQUE,
    first_name VARCHAR(128)
        NOT NULL,
    last_name VARCHAR(128)
        NOT NULL,
    bio VARCHAR(512),
    created_at TIMESTAMPTZ
        NOT NULL
);

CREATE TABLE event_locations
(
    id UUID
        CONSTRAINT pk_event_locations PRIMARY KEY,
    country_code VARCHAR(3)
        NOT NULL
        CONSTRAINT fk_event_locations_country_code REFERENCES country_codes (code),
    venue_name VARCHAR(64),
    building_num VARCHAR(16),
    street VARCHAR(128),
    postal_code VARCHAR(16)
        NOT NULL,
    city VARCHAR(128)
        NOT NULL,
    coordinates geography(point, 4326)
        NOT NULL,
    created_at TIMESTAMPTZ
        NOT NULL
);

CREATE TABLE media
(
    id UUID
        CONSTRAINT pk_media PRIMARY KEY,
    owner_id UUID
        NOT NULL
        CONSTRAINT fk_media_owner_id REFERENCES users (id),
    object_key TEXT
        NOT NULL
        CONSTRAINT uq_media_object_key UNIQUE,
    file_name VARCHAR(255)
        NOT NULL,
    purpose VARCHAR(64)
        NOT NULL
        CONSTRAINT ck_media_purpose CHECK (
            purpose IN (
                        'profile_image',
                        'profile_image_thumbnail',
                        'event_cover',
                        'event_media'
                )
            ),
    mime_type VARCHAR(64)
        NOT NULL,
    size_bytes BIGINT
        NOT NULL,
    status VARCHAR(32)
        NOT NULL
        CONSTRAINT ck_media_status CHECK (
            status IN (
                       'PENDING',
                       'UPLOADED',
                       'ATTACHED',
                       'DELETED'
                )
            ),
    created_at TIMESTAMPTZ
        NOT NULL,
    deleted_at TIMESTAMPTZ
);

CREATE TABLE profile_images
(
    id UUID
        CONSTRAINT pk_profile_images PRIMARY KEY,
    user_id UUID
        NOT NULL
        CONSTRAINT fk_profile_images_user_id REFERENCES users (id),
    full_media_id UUID
        NOT NULL
        CONSTRAINT fk_profile_images_full_media_id REFERENCES media (id),
    thumbnail_media_id UUID
        NOT NULL
        CONSTRAINT fk_profile_images_thumbnail_media_id REFERENCES media (id),
    is_active BOOLEAN
        NOT NULL,
    set_at TIMESTAMPTZ
        NOT NULL,
    unset_at TIMESTAMPTZ
);

CREATE TABLE posts
(
    id UUID
        CONSTRAINT pk_posts PRIMARY KEY,
    author_id UUID
        NOT NULL
        CONSTRAINT fk_posts_author_id REFERENCES users (id),
    event_category_id UUID
        NOT NULL
        CONSTRAINT fk_posts_event_category_id REFERENCES event_categories (id),
    event_location_id UUID
        CONSTRAINT fk_posts_event_location_id REFERENCES event_locations (id),
    title VARCHAR(32)
        NOT NULL,
    description VARCHAR(1024)
        NOT NULL,
    event_url VARCHAR(255),
    tags VARCHAR(32)[],
    type VARCHAR(32)
        NOT NULL
        CONSTRAINT ck_posts_type CHECK (
            type IN (
                     'ONLINE',
                     'OFFLINE'
                )
            ),
    status VARCHAR(32)
        NOT NULL
        CONSTRAINT ck_posts_status CHECK (
            status IN (
                       'ACTIVE',
                       'EDITED',
                       'DELETED'
                )
            ),
    visibility VARCHAR(32)
        NOT NULL
        CONSTRAINT ck_posts_visibility CHECK (
            visibility IN (
                           'PUBLIC',
                           'PRIVATE',
                           'FRIENDS'
                )
            ),
    positive_reaction_count INTEGER
        NOT
            NULL
        CONSTRAINT ck_posts_positive_reaction_count_nonneg CHECK (
            positive_reaction_count >= 0
            ),
    negative_reaction_count INTEGER
        NOT NULL
        CONSTRAINT ck_posts_negative_reaction_count_nonneg CHECK (
            negative_reaction_count >= 0
            ),
    participant_count INTEGER
        NOT NULL
        CONSTRAINT ck_posts_participant_count_nonneg CHECK (
            participant_count >= 0
            ),
    comments_count INTEGER
        NOT NULL
        CONSTRAINT ck_posts_comments_count_nonneg CHECK (
            comments_count >= 0
            ),
    starts_at TIMESTAMPTZ
        NOT NULL,
    ends_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ
        NOT NULL,
    last_modified_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,

    CONSTRAINT ck_posts_starts_after_created CHECK (
        starts_at > created_at
        ),
    CONSTRAINT ck_posts_ends_after_starts CHECK (
        ends_at IS NULL
            OR ends_at > starts_at
        ),
    CONSTRAINT ck_posts_location_matches_type CHECK (
        (
            type = 'ONLINE'
                AND event_location_id IS NULL
            )
            OR
        (
            type = 'OFFLINE'
                AND event_location_id IS NOT NULL
            )
        )
);

CREATE TABLE comments
(
    id UUID
        CONSTRAINT pk_comments PRIMARY KEY,
    post_id UUID
        NOT NULL
        CONSTRAINT fk_comments_post_id REFERENCES posts (id),
    author_id UUID
        NOT NULL
        CONSTRAINT fk_comments_author_id REFERENCES users (id),
    root_comment_id UUID
        CONSTRAINT fk_comments_root_comment_id REFERENCES comments (id),
    parent_comment_id UUID
        CONSTRAINT fk_comments_parent_comment_id REFERENCES comments (id),
    ancestor_ids UUID[],
    content VARCHAR(512)
        NOT NULL,
    status VARCHAR(32)
        NOT NULL
        CONSTRAINT ck_comments_status CHECK (
            status IN (
                       'ACTIVE',
                       'DELETED'
                )
            ),
    replies_count INTEGER
        NOT NULL
        CONSTRAINT ck_comments_replies_count_nonneg CHECK (
            replies_count >= 0
            ),
    created_at TIMESTAMPTZ
        NOT NULL,
    last_edited_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ
);

CREATE TABLE post_media
(
    id UUID
        CONSTRAINT pk_post_media PRIMARY KEY,
    post_id UUID
        NOT NULL
        CONSTRAINT fk_post_media_post_id REFERENCES posts (id),
    media_id UUID
        NOT NULL
        CONSTRAINT fk_post_media_media_id REFERENCES media (id),
    position INTEGER
        NOT NULL
        CONSTRAINT ck_post_media_position_nonneg CHECK (
            position >= 0
            ),
    is_cover BOOLEAN
        NOT NULL
);

CREATE TABLE post_reactions
(
    post_id UUID
        NOT NULL
        CONSTRAINT fk_post_reactions_post_id REFERENCES posts (id),
    user_id UUID
        NOT NULL
        CONSTRAINT fk_post_reactions_user_id REFERENCES users (id),
    type VARCHAR(32)
        NOT NULL
        CONSTRAINT ck_post_reactions_type CHECK (
            type IN (
                     'like',
                     'dislike'
                )
            ),
    created_at TIMESTAMPTZ
        NOT NULL,

    CONSTRAINT pk_post_reactions PRIMARY KEY (post_id, user_id)
);

CREATE TABLE post_participations
(
    post_id UUID
        NOT NULL
        CONSTRAINT fk_post_participations_post_id REFERENCES posts (id),
    user_id UUID
        NOT NULL
        CONSTRAINT fk_post_participations_user_id REFERENCES users (id),
    type VARCHAR(32)
        NOT NULL
        CONSTRAINT ck_post_participations_type CHECK (
            type IN (
                     'interested',
                     'takes_part'
                )
            ),
    created_at TIMESTAMPTZ
        NOT NULL,

    CONSTRAINT pk_post_participations PRIMARY KEY (post_id, user_id)
);

CREATE TABLE user_relations
(
    source_user_id UUID
        NOT NULL
        CONSTRAINT fk_user_relations_source_user_id REFERENCES users (id),
    target_user_id UUID
        NOT NULL
        CONSTRAINT fk_user_relations_target_user_id REFERENCES users (id),
    type VARCHAR(32)
        NOT NULL
        CONSTRAINT ck_user_relations_type CHECK (
            type IN (
                     'FOLLOW',
                     'BLOCK')
            ),
    created_at TIMESTAMPTZ
        NOT NULL,

    CONSTRAINT pk_user_relations PRIMARY KEY (source_user_id, target_user_id)
);