package pl.coolture.restapi.post.api.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

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

    private List<@Size(max = 32) String> tags;

    private UUID authorId;

    private String status;

    private String visibility;

    private String type;

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
    private List<@Pattern(regexp = "interested|takes_part") String> participationTypes;

    private String cursor;

    @Min(1) @Max(100)
    private int limit = 20;
}
