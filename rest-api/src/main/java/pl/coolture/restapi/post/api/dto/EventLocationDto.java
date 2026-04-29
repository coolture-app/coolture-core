package pl.coolture.restapi.post.api.dto;

import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

import static com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY;

@Builder
public record EventLocationDto(
        /**
         * Jackson will ignore this id on input
         * but serializes it on output.
         * This will protect by accidental MapStruct
         * overwrites on updates (e.g. PATCH /api/posts).
         *
         * Covers need to create separate request & response records.
         */
        @JsonProperty(access = READ_ONLY)
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
        GeoPointDto coordinates,

        @JsonProperty(access = READ_ONLY)
        Instant createdAt
) {}