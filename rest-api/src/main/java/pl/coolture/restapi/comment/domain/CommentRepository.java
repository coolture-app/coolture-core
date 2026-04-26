package pl.coolture.restapi.comment.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    /**
     * Page of root-level comments for a post (parent_comment_id IS NULL).
     * Cursor pagination on (created_at, id) DESC
     */
    @Query(value = """
            SELECT c.* FROM comments c
            WHERE c.post_id = CAST(:postId AS uuid)
              AND c.parent_comment_id IS NULL
              AND (CAST(:cursorCreatedAt AS timestamptz) IS NULL
                   OR c.created_at < CAST(:cursorCreatedAt AS timestamptz)
                   OR (c.created_at = CAST(:cursorCreatedAt AS timestamptz)
                       AND c.id < CAST(:cursorId AS uuid)))
            ORDER BY c.created_at DESC, c.id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Comment> findRootCommentsForPost(
            @Param("postId")          UUID postId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId")        UUID cursorId,
            @Param("limit")           int limit);

    /**
     * Page of direct replies for a single parent comment.
     * Same cursor pagination scheme as for finding root comments
     */
    @Query(value = """
            SELECT c.* FROM comments c
            WHERE c.parent_comment_id = CAST(:parentId AS uuid)
              AND (CAST(:cursorCreatedAt AS timestamptz) IS NULL
                   OR c.created_at < CAST(:cursorCreatedAt AS timestamptz)
                   OR (c.created_at = CAST(:cursorCreatedAt AS timestamptz)
                       AND c.id < CAST(:cursorId AS uuid)))
            ORDER BY c.created_at DESC, c.id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Comment> findRepliesForParent(
            @Param("parentId")        UUID parentId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId")        UUID cursorId,
            @Param("limit")           int limit);

    /**
     * Bumps replies_count on every ancestor in the given chain at once.
     * Used when a new comment is created - the chain is exactly the new
     * comment's ancestor_ids (parent, grandparent, ...), so the root comment
     * counts the whole subtree below it, not just its direct children.
     */
    @Modifying
    @Query("UPDATE Comment c SET c.repliesCount = c.repliesCount + 1 WHERE c.id IN :ids")
    void incrementRepliesCountForAll(@Param("ids") List<UUID> ids);

    /**
     * Symmetric to incrementRepliesCountForAll
     */
    @Modifying
    @Query("UPDATE Comment c SET c.repliesCount = c.repliesCount - 1 " +
            "WHERE c.id IN :ids AND c.repliesCount > 0")
    void decrementRepliesCountForAll(@Param("ids") List<UUID> ids);

    @Modifying
    @Query("DELETE FROM Comment c WHERE c.post.id = :postId")
    void deleteAllForPost(@Param("postId") UUID postId);
}