package pl.coolture.restapi.post.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record EventLocationDto(
        UUID id,

        @NotBlank
        @Size(min = 3, max = 3)
        String countryCode,

        @Size(max = 64)
        String venueName,

        @Size(max = 16)
        String buildingNum,

        @Size(max = 128)
        String street,

        @NotBlank
        @Size(max = 16)
        String postalCode,

        @NotBlank
        @Size(max = 128)
        String city,

        @NotNull
        @Valid
        GeoPointDto coordinates) {}