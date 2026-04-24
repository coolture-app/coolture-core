package pl.coolture.restapi.common.exceptions;

/**
 * Thrown when a request conflicts with the current state of a resource
 * Maps to HTTP 409 Conflict.
 * examples:
 *   - deleting media that is still attached to an active post or profile image
 *   - calling complete() on a media record that is already in UPLOADED state
 *   - attempting to follow a user that is already followed
 */
public class ConflictException extends DomainException {

    private static final int HTTP_STATUS = 409;

    public ConflictException(String message) {
        super(message, HTTP_STATUS);
    }
}