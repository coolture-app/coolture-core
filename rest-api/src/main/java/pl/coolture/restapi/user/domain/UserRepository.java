package pl.coolture.restapi.user.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

  Optional<User> findByUsername(String username);

  /**
   * Case-insensitive substring search across username, first name and last name,
   * with cursor pagination ordered by (created_at DESC, id DESC)
   *
   * Why native query
   * Hibernate cannot infer the SQL type of a named parameter when its value is null.
   * In JPQL this causes PostgreSQL to receive an untyped '?' placeholder and reject
   * the query with "function lower(bytea) does not exist" or
   * "could not determine data type of parameter $N".
   * Wrapping every nullable parameter in an explicit CAST resolves the type at
   * parse time, regardless of whether the runtime value is null.
   *
   * Why CAST(x) IS NULL instead of x IS NULL
   * The IS NULL guard must be on the cast expression, not the bare placeholder,
   * otherwise PostgreSQL still cannot determine the type during query planning.
   *
   * @param q               substring to search; null means "return all users"
   * @param cursorCreatedAt created_at of the last item on the previous page; null for page 1
   * @param cursorId        id of the last item on the previous page; null for page 1
   * @param limit           maximum number of rows to return (pass limit + 1 for hasMore probe)
   */
  @Query(value = """
            SELECT u.*,
                   (SELECT COUNT(*) FROM user_relations ur
                    WHERE ur.target_user_id = u.id AND ur.type = 'FOLLOW') AS "followersCount",
                   (SELECT COUNT(*) FROM user_relations ur
                    WHERE ur.source_user_id = u.id AND ur.type = 'FOLLOW') AS "followingCount"
            FROM users u
            WHERE (
                CAST(:q AS varchar) IS NULL
                OR LOWER(u.username)   LIKE LOWER('%' || CAST(:q AS varchar) || '%')
                OR LOWER(u.first_name) LIKE LOWER('%' || CAST(:q AS varchar) || '%')
                OR LOWER(u.last_name)  LIKE LOWER('%' || CAST(:q AS varchar) || '%')
            )
            AND (
                CAST(:cursorCreatedAt AS timestamptz) IS NULL
                OR u.created_at < CAST(:cursorCreatedAt AS timestamptz)
                OR (u.created_at = CAST(:cursorCreatedAt AS timestamptz)
                    AND u.id < CAST(:cursorId AS uuid))
            )
            ORDER BY u.created_at DESC, u.id DESC
            LIMIT :limit
            """, nativeQuery = true)
  List<User> search(
          @Param("q") String q,
          @Param("cursorCreatedAt") Instant cursorCreatedAt,
          @Param("cursorId") UUID cursorId,
          @Param("limit") int limit);
}