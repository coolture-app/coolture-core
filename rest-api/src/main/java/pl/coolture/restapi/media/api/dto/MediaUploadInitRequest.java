package pl.coolture.restapi.media.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record MediaUploadInitRequest(
        @NotBlank String purpose,
        @NotBlank @Size(max = 64) String mimeType,
        @NotNull @Positive Long sizeBytes,
        @NotBlank @Size(max = 255) String fileName) {}
