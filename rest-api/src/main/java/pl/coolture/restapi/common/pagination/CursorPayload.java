package pl.coolture.restapi.common.pagination;

import java.time.Instant;
import java.util.UUID;

/**
 * Decoded pagination cursor
 *
 * Keyset pagination uses the (createdAt, id)
 * as the exclusive lower/upper bound in the WHERE clause:
 * e.g., WHERE (created_at, id) < (:createdAt, :id)
 *
 * When multiple rows have the same timestamp (createdAt alone is not unique)
 * then id acts as tiebreaker bcs it's always unique
 *
 * @param id        UUID of the last item on the previous page
 * @param createdAt creation timestamp of the last item on the previous page
 */
public record CursorPayload(UUID id, Instant createdAt) {}