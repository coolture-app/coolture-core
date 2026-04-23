package pl.coolture.restapi.common.pagination;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

/**
 * Pagination metadata included in every paginated API response
 * Matches PaginationMeta defined in the API contract.
 *
 * nextCursor is null when there are no more pages:
 * JsonInclude(NON_NULL) keeps it out of the body
 * clients can use a null-check instead of comparing against some value
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaginationMeta {

    // Number of items requested by client
    private final int limit;

    // True when at least one more page exists after the current one
    private final boolean hasMore;
    
    // Opaque cursor to pass as the query parameter on the next request
    // Absent from the response when hasMore is false
    private final String nextCursor;
}