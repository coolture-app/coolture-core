package pl.coolture.restapi.post.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PostCreateRequest(
        @NotNull
        UUID categoryId,

        @NotBlank
        @Size(max = 32)
        String title,

        @NotBlank
        @Size(max = 1024)
        String description,

        @Size(max = 2048)
        String eventUrl,

        @NotNull
        @Future
        Instant startsAt,

        Instant endsAt,

        @Size(max = 10)
        List<@Size(max = 32) String> tags,

        @NotNull
        @Pattern(regexp = "OFFLINE|ONLINE")
        String type,

        @Pattern(regexp = "PUBLIC|PRIVATE|FRIENDS")
        String visibility,

        @Valid EventLocationDto
        location,

        List<UUID> mediaIds,

        UUID coverMediaId) {}