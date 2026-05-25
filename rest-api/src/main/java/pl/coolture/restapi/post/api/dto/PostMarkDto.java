package pl.coolture.restapi.post.api.dto;

import java.util.UUID;

public record PostMarkDto(
        UUID id,
        String title,
        String description,
        String coverMediaUrl,
        int positiveReactionCount,
        GeoPointDto coordinates) {}
