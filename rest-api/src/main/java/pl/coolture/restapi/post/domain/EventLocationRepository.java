package pl.coolture.restapi.post.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventLocationRepository extends JpaRepository<EventLocation, UUID> {

    @Query("SELECT (count(p) > 0) FROM Post p WHERE p.location.id = :locationId")
    boolean isReferencedByPost(@Param("locationId") UUID locationId);

    @Query(
            value =
            """
            SELECT * FROM event_locations el
            WHERE (CAST(:cursorCreatedAt AS timestamptz) IS NULL
                   OR el.created_at < CAST(:cursorCreatedAt AS timestamptz)
                   OR (el.created_at = CAST(:cursorCreatedAt AS timestamptz)
                       AND el.id < CAST(:cursorId AS uuid)))
            ORDER BY el.created_at DESC, el.id DESC
            LIMIT :limit
            """,
            nativeQuery = true)
    List<EventLocation> findPage(
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            @Param("limit") int limit);
}