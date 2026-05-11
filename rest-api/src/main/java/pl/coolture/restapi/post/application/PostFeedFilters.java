package pl.coolture.restapi.post.application;

import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import pl.coolture.restapi.post.domain.PostStatus;
import pl.coolture.restapi.post.domain.PostType;
import pl.coolture.restapi.post.domain.PostVisibility;

/**
 * Container for every filter param of GET /posts.
 * Should be created in the controller from PostFeedRequest
 * in order to isolate service from knowing about HTTP
 */
@Builder
public record PostFeedFilters(
        String          q,
        UUID            categoryId,
        List<String>    tags,
        UUID            authorId,
        PostStatus      status,
        PostVisibility  visibility,
        PostType        type,
        Instant         startsFrom,
        Instant         startsTo,
        Double          latitude,
        Double          longitude,
        Double          radiusKm,
        List<String>    participationTypes,
        String          reactionType,
        String          sortBy
) {}