package pl.coolture.restapi.media;

import java.time.LocalDateTime;
import java.util.UUID;

public record ImageUploadResponse(UUID id, String s3Key, String ownerId, LocalDateTime createdAt) {}
