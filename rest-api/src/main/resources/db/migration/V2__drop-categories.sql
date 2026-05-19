ALTER TABLE posts DROP CONSTRAINT fk_posts_event_category_id;
ALTER TABLE posts DROP COLUMN event_category_id;
DROP TABLE event_categories;