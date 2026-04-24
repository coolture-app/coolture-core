package pl.coolture.restapi.media.api.dto;

import java.time.Instant;
import java.util.UUID;

public record ProfileImageResponse(
        UUID fullMediaId,
        UUID thumbnailMediaId,
        Instant setAt) {}
