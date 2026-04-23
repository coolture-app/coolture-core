package pl.coolture.restapi.common.pagination;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * Wrapper for a single page of cursor-paginated results.
 *
 * Controllers return this directly - Jackson serialises it to:
 * {
 *  "items": [...],
 *  "page": { "limit": 20, "hasMore": true, "nextCursor": "eyJ..." }
 * }
 *
 * How to build a page in a service method:
 *
 *  List<Post> rows = postRepository.findPage(cursorPayload, limit + 1);
 *  return CursorPage.of(rows, limit, Post::getId, Post::getCreatedAt, codec);
 *
 * The repository always fetches limit + 1 rows.
 * If the extra row exists, hasMore is true, and it is stripped from the returned items list.
 *
 * @param <T> the item type exposed to the API
 */
public record CursorPage<T>(List<T> items, PaginationMeta page) {

    /**
     * Builds a CursorPage from a raw result list returned by the repository.
     *
     * @param rows                  repository result
     * @param limit                 the page size requested by the client
     * @param idExtractor           function to read the UUID from the last item
     * @param timestampExtractor    function to read the creation timestamp from the last item
     * @param codec                 encoder used to produce the next cursor string
     */
    public static <T> CursorPage<T> of(
            List<T> rows,
            int limit,
            Function<T, UUID> idExtractor,
            Function<T, Instant> timestampExtractor,
            CursorCodec codec) {

        boolean hasMore = rows.size() > limit;

        // drop +1 row (it is only for checking hasMore)
        List<T> items = hasMore ? rows.subList(0, limit) : rows;

        String nextCursor = null;
        if (hasMore) {
            T last = items.getLast();
            nextCursor = codec.encode(new CursorPayload(idExtractor.apply(last), timestampExtractor.apply(last)));
        }

        PaginationMeta meta = PaginationMeta.builder()
                .limit(limit)
                .hasMore(hasMore)
                .nextCursor(nextCursor)
                .build();

        return new CursorPage<>(items, meta);
    }
}