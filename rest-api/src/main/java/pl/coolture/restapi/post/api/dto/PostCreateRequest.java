package pl.coolture.restapi.post.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import pl.coolture.restapi.post.domain.PostType;
import pl.coolture.restapi.post.domain.PostVisibility;

public record PostCreateRequest(
        @NotBlank
        @Size(max = 128)
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
        PostType type,

        PostVisibility visibility,

        @Valid EventLocationDto
        location,

        List<UUID> mediaIds,

        UUID coverMediaId) {}