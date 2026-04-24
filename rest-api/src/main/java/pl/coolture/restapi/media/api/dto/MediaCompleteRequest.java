package pl.coolture.restapi.media.api.dto;

/**
 * Optional body for `complete` endpoint
 * etag is the value returned by S3 in the Entity Tag response header after a successful PUT.
 * Providing it allows the server to verify the file was not corrupted in transit.
 * Omitting it skips the verification.
 */
public record MediaCompleteRequest(String etag) {}
