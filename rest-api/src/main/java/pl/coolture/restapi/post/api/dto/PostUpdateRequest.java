package pl.coolture.restapi.post.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * All fields optional. Null do not change previous values
 */
public record PostUpdateRequest(
        UUID categoryId,

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

        @Pattern(regexp = "OFFLINE|ONLINE")
        String type,

        @Pattern(regexp = "PUBLIC|PRIVATE|FRIENDS")
        String visibility,

        @Valid
        EventLocationDto location,

        List<UUID> mediaIds,

        UUID       coverMediaId) {}