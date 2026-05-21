package pl.coolture.restapi.post.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import pl.coolture.restapi.post.domain.PostType;
import pl.coolture.restapi.post.domain.PostVisibility;

/**
 * All fields optional. Null do not change previous values
 */
public record PostUpdateRequest(
        @Size(max = 32)
        String title,

        @Size(max = 1024)
        String description,

        @Size(max = 2048)
        String eventUrl,

        Instant startsAt,

        Instant endsAt,

        @Size(max = 10)
        List<@Size(max = 32) String> tags,

        PostType type,

        PostVisibility visibility,

        @Valid
        EventLocationDto location,

        List<UUID> mediaIds,

        UUID       coverMediaId) {}