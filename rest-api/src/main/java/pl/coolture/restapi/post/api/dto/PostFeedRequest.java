package pl.coolture.restapi.post.api.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import pl.coolture.restapi.post.application.PostFeedFilters;
import pl.coolture.restapi.post.domain.PostStatus;
import pl.coolture.restapi.post.domain.PostType;
import pl.coolture.restapi.post.domain.PostVisibility;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Separate from PostFeedFilters record in application layer
 * because it has other purpose: receive params from URL
 * this is why it has Spring annotations & validation annotations
 *
 * It removes need for X different @RequestParam in GET /api/posts
 */
@Getter
@Setter
public class PostFeedRequest {
    /**
     * text search in titles & descriptions
     */
    @Size(max = 100)
    private String q;

    private UUID categoryId;

    @ArraySchema(schema = @Schema(type = "string", maxLength = 32))
    private List<@Size(max = 32) String> tags;

    private UUID authorId;

    /**
     * TODO: only user with ADMIN role
     *  should be able to get posts with status 'DELETED'
     */
    private PostStatus status;

    /**
     * TODO: request with visibility set to PRIVATE or FRIENDS
     *  should throw HTTP 401 when unauthed user
     */
    private PostVisibility visibility;

    private PostType type;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant startsFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant startsTo;

    @DecimalMin("-90.0") @DecimalMax("90.0")
    private Double latitude;

    @DecimalMin("-180.0") @DecimalMax("180.0")
    private Double longitude;

    @DecimalMin("0.1") @DecimalMax("500.0")
    private Double radiusKm;

    /**
     * Filter posts in respect to caller's participation status
     * Require auth - without valid JWT results in HTTP 401
     */
    @ArraySchema(schema = @Schema(type = "string", pattern = "interested|takes_part"))
    private List<@Pattern(regexp = "interested|takes_part") String> participationTypes;

    /**
     * Filter posts in respect to caller's reaction
     * Require auth - without valid JWT result in HTTP 401
     */
    @Pattern(regexp = "LIKE|DISLIKE")
    private String reactionType;

    private String cursor;

    @Min(1) @Max(100)
    private int limit = 20;

    @Pattern(regexp = "RECENT|POPULAR|UPCOMING")
    private String sortBy;

    /**
     * to avoid mapping fields one by one that is prune to mistakes
     */
    public PostFeedFilters toFilters() {
        return PostFeedFilters.builder()
                .q(q)
                .authorId(authorId)
                .categoryId(categoryId)
                .tags(tags)
                .type(type)
                .reactionType(reactionType)
                .participationTypes(participationTypes)
                .latitude(latitude)
                .longitude(longitude)
                .radiusKm(radiusKm)
                .visibility(visibility)
                .status(status)
                .startsFrom(startsFrom)
                .startsTo(startsTo)
                .sortBy(sortBy)
                .build();
    }
}
