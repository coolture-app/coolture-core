package pl.coolture.restapi.post.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Container for every filter param of GET /posts.
 */
public record PostFeedFilters(
        String     q,
        UUID       categoryId,
        List<String> tags,
        UUID       authorId,
        String     status,
        String     visibility,
        String     type,
        Instant    startsFrom,
        Instant    startsTo,
        Double     latitude,
        Double     longitude,
        Double     radiusKm) {}