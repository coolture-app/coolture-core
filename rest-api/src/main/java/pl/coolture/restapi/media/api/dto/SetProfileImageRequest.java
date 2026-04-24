package pl.coolture.restapi.media.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SetProfileImageRequest(
        @NotNull UUID fullMediaId,
        @NotNull UUID thumbnailMediaId) {}
