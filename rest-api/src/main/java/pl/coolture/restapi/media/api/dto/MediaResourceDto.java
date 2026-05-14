package pl.coolture.restapi.media.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;
import pl.coolture.restapi.media.domain.MediaPurpose;
import pl.coolture.restapi.media.domain.MediaStatus;

/**
 * API representation of a media file with a pre-signed GET URL.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MediaResourceDto(
        UUID id,
        MediaPurpose purpose,
        String mimeType,
        Long sizeBytes,
        MediaStatus status,
        /** Time-limited pre-signed GET URL pointing directly at S3 resource. */
        String url,
        Instant createdAt,
        Instant deletedAt) {}