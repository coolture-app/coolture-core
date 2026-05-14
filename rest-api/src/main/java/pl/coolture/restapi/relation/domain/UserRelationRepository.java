package pl.coolture.restapi.relation.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRelationRepository extends JpaRepository<UserRelation, UserRelationId> {

    Optional<UserRelation> findById(UserRelationId id);

    boolean existsByIdAndType(UserRelationId id, UserRelationType type);

    /** Deletes a specific relation type between two users
     * (e.g. remove only FOLLOW, not BLOCK). */
    @Modifying
    @Query("DELETE FROM UserRelation ur WHERE ur.id = :id AND ur.type = :type")
    int deleteByIdAndType(@Param("id") UserRelationId id, @Param("type") UserRelationType type);

    /** Removes any relation in the reverse direction
     * used when blocking to clear reverse follows. */
    @Modifying
    @Query("DELETE FROM UserRelation ur WHERE ur.id.sourceUserId = :sourceId AND ur.id.targetUserId = :targetId")
    void deleteAny(@Param("sourceId") UUID sourceId, @Param("targetId") UUID targetId);

    /**
     * Paginated followers, users who follow :userId
     * Cursor is on (createdAt, sourceUserId)
     */
    @Query(value = """
            SELECT ur.* FROM user_relations ur
            WHERE ur.target_user_id = CAST(:userId AS uuid)
              AND ur.type = 'FOLLOW'
              AND (
                CAST(:cursorCreatedAt AS timestamptz) IS NULL
                OR ur.created_at < CAST(:cursorCreatedAt AS timestamptz)
                OR (ur.created_at = CAST(:cursorCreatedAt AS timestamptz)
                    AND ur.source_user_id < CAST(:cursorId AS uuid))
              )
            ORDER BY ur.created_at DESC, ur.source_user_id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<UserRelation> findFollowers(
            @Param("userId") UUID userId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            @Param("limit") int limit);

    /**
     * Paginated following, users that :userId follows
     * Cursor is on (createdAt, targetUserId).
     */
    @Query(value = """
            SELECT ur.* FROM user_relations ur
            WHERE ur.source_user_id = CAST(:userId AS uuid)
              AND ur.type = 'FOLLOW'
              AND (
                CAST(:cursorCreatedAt AS timestamptz) IS NULL
                OR ur.created_at < CAST(:cursorCreatedAt AS timestamptz)
                OR (ur.created_at = CAST(:cursorCreatedAt AS timestamptz)
                    AND ur.target_user_id < CAST(:cursorId AS uuid))
              )
            ORDER BY ur.created_at DESC, ur.target_user_id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<UserRelation> findFollowing(
            @Param("userId") UUID userId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            @Param("limit") int limit);

    /**
     * Paginated blocking, users blocked by :userId.
     * Cursor is on (createdAt, targetUserId).
     */
    @Query(value = """
            SELECT ur.* FROM user_relations ur
            WHERE ur.source_user_id = CAST(:userId AS uuid)
              AND ur.type = 'BLOCK'
              AND (
                CAST(:cursorCreatedAt AS timestamptz) IS NULL
                OR ur.created_at < CAST(:cursorCreatedAt AS timestamptz)
                OR (ur.created_at = CAST(:cursorCreatedAt AS timestamptz)
                    AND ur.target_user_id < CAST(:cursorId AS uuid))
              )
            ORDER BY ur.created_at DESC, ur.target_user_id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<UserRelation> findBlocking(
            @Param("userId") UUID userId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            @Param("limit") int limit);
}