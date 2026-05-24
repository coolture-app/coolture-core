package pl.coolture.restapi.post.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, UUID> {

  /**
   * Feed query with all optional filters and cursor pagination on (created_at, id).
   *
   * <p>Tags filter uses PostgreSQL's && (array overlap) operator.
   *
   * <p>Participation filter: When participationTypes is non-null, only posts for which the given
   * callerId has a matching entry in post_participations are returned. The service guarantees
   * callerId is non-null whenever participationTypes is set.
   *
   * <p>Geo filter applies only when all three of lat, lng, radiusMeters are present. ST_DWithin on
   * geography type measures distance in meters along the earth surface. Posts without a location
   * are excluded from the geo-filtered result.
   */
  @Query(
      value =
          """
          SELECT p.* FROM posts p
          LEFT JOIN event_locations el ON el.id = p.event_location_id
          WHERE p.deleted_at IS NULL
            AND (CAST(:q AS varchar) IS NULL
                 OR LOWER(p.title)       LIKE LOWER('%' || CAST(:q AS varchar) || '%')
                 OR LOWER(p.description) LIKE LOWER('%' || CAST(:q AS varchar) || '%'))
            AND (CAST(:tags AS varchar[])    IS NULL OR p.tags && CAST(:tags AS varchar[]))
            AND (CAST(:authorId AS uuid)     IS NULL OR p.author_id         = CAST(:authorId AS uuid))
            AND (CAST(:status AS varchar)    IS NULL OR p.status            = CAST(:status AS varchar))
            AND (CAST(:visibility AS varchar)IS NULL OR p.visibility        = CAST(:visibility AS varchar))
            AND (CAST(:type AS varchar)      IS NULL OR p.type              = CAST(:type AS varchar))
            AND (CAST(:startsFrom AS timestamptz) IS NULL OR p.starts_at >= CAST(:startsFrom AS timestamptz))
            AND (CAST(:startsTo   AS timestamptz) IS NULL OR p.starts_at <= CAST(:startsTo   AS timestamptz))
            AND (CAST(:lat AS double precision) IS NULL
                 OR CAST(:lng AS double precision) IS NULL
                 OR CAST(:radiusMeters AS double precision) IS NULL
                 OR (el.coordinates IS NOT NULL
                     AND ST_DWithin(
                           el.coordinates,
                           ST_SetSRID(ST_MakePoint(
                               CAST(:lng AS double precision),
                               CAST(:lat AS double precision)), 4326)::geography,
                           CAST(:radiusMeters AS double precision))))
            AND (CAST(:participationTypes AS varchar[]) IS NULL
                 OR EXISTS (
                     SELECT 1 FROM post_participations pp
                     WHERE pp.post_id  = p.id
                       AND pp.user_id  = CAST(:callerId AS uuid)
                       AND pp.type     = ANY(CAST(:participationTypes AS varchar[]))))
            AND (CAST(:reactionType AS varchar) IS NULL
                 OR EXISTS (
                     SELECT 1 FROM post_reactions pr
                     WHERE pr.post_id  = p.id
                       AND pr.user_id  = CAST(:callerId AS uuid)
                       AND pr.type::varchar = CAST(:reactionType AS varchar)))
            AND (CAST(:cursorCreatedAt AS timestamptz) IS NULL
                 OR p.created_at < CAST(:cursorCreatedAt AS timestamptz)
                 OR (p.created_at = CAST(:cursorCreatedAt AS timestamptz)
                     AND p.id < CAST(:cursorId AS uuid)))
            ORDER BY
            CASE WHEN :sortBy = 'popular' THEN p.positive_reaction_count END DESC,
            CASE WHEN :sortBy = 'upcoming' THEN p.starts_at END ASC,
            p.created_at DESC,
            p.id DESC
          LIMIT :limit
          """,
      nativeQuery = true)
  List<Post> findFeed(
      @Param("q") String q,
      @Param("tags") String[] tags,
      @Param("authorId") UUID authorId,
      @Param("status") String status,
      @Param("visibility") String visibility,
      @Param("type") String type,
      @Param("startsFrom") Instant startsFrom,
      @Param("startsTo") Instant startsTo,
      @Param("lat") Double lat,
      @Param("lng") Double lng,
      @Param("radiusMeters") Double radiusMeters,
      @Param("participationTypes") String[] participationTypes,
      @Param("reactionType") String reactionType,
      @Param("callerId") UUID callerId,
      @Param("cursorCreatedAt") Instant cursorCreatedAt,
      @Param("cursorId") UUID cursorId,
      @Param("sortBy") String sortBy,
      @Param("limit") int limit);

  @Query(
          value =
                  """
                  SELECT p.* FROM posts p
                  JOIN event_locations el ON el.id = p.event_location_id
                  WHERE p.deleted_at IS NULL
                    AND el.coordinates IS NOT NULL
                    AND (CAST(:q AS varchar) IS NULL
                         OR LOWER(p.title)       LIKE LOWER('%' || CAST(:q AS varchar) || '%')
                         OR LOWER(p.description) LIKE LOWER('%' || CAST(:q AS varchar) || '%'))
                    AND (CAST(:tags AS varchar[])    IS NULL OR p.tags && CAST(:tags AS varchar[]))
                    AND (CAST(:authorId AS uuid)     IS NULL OR p.author_id         = CAST(:authorId AS uuid))
                    AND (CAST(:status AS varchar)    IS NULL OR p.status            = CAST(:status AS varchar))
                    AND (CAST(:visibility AS varchar)IS NULL OR p.visibility        = CAST(:visibility AS varchar))
                    AND (CAST(:type AS varchar)      IS NULL OR p.type              = CAST(:type AS varchar))
                    AND (CAST(:startsFrom AS timestamptz) IS NULL OR p.starts_at >= CAST(:startsFrom AS timestamptz))
                    AND (CAST(:startsTo   AS timestamptz) IS NULL OR p.starts_at <= CAST(:startsTo   AS timestamptz))
                    AND (el.coordinates::geometry && ST_MakeEnvelope(
                         CAST(:minLng AS double precision),
                         CAST(:minLat AS double precision),
                         CAST(:maxLng AS double precision),
                         CAST(:maxLat AS double precision),
                         4326))
                    AND (CAST(:participationTypes AS varchar[]) IS NULL
                         OR EXISTS (
                             SELECT 1 FROM post_participations pp
                             WHERE pp.post_id  = p.id
                               AND pp.user_id  = CAST(:callerId AS uuid)
                               AND pp.type     = ANY(CAST(:participationTypes AS varchar[]))))
                    AND (CAST(:reactionType AS varchar) IS NULL
                         OR EXISTS (
                             SELECT 1 FROM post_reactions pr
                             WHERE pr.post_id  = p.id
                               AND pr.user_id  = CAST(:callerId AS uuid)
                               AND pr.type::varchar = CAST(:reactionType AS varchar)))
                  """,
          nativeQuery = true)
  List<Post> findAllMapMarks(
          @Param("q") String q,
          @Param("tags") String[] tags,
          @Param("authorId") UUID authorId,
          @Param("status") String status,
          @Param("visibility") String visibility,
          @Param("type") String type,
          @Param("startsFrom") Instant startsFrom,
          @Param("startsTo") Instant startsTo,
          @Param("minLng") Double minLng,
          @Param("minLat") Double minLat,
          @Param("maxLng") Double maxLng,
          @Param("maxLat") Double maxLat,
          @Param("participationTypes") String[] participationTypes,
          @Param("reactionType") String reactionType,
          @Param("callerId") UUID callerId);
}
