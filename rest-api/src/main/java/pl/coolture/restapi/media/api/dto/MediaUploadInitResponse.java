package pl.coolture.restapi.media.api.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Client must PUT the file binary to uploadUrl with the requiredHeaders included,
 * then call `complete` endpoint
 */
public record MediaUploadInitResponse(
        UUID mediaId,
        String objectKey,
        /** Presigned PUT URL pointing at the public address. */
        String uploadUrl,
        String httpMethod,
        Instant expiresAt,
        /** Headers that must be sent with the PUT request (e.g. Content-Type, Content-Length). */
        Map<String, String> requiredHeaders) {}
