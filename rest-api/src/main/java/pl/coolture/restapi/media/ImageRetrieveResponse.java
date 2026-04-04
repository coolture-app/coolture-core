package pl.coolture.restapi.media;

import java.util.UUID;

public record ImageRetrieveResponse(UUID id, UUID ownerId, String s3Key, String url) {}
