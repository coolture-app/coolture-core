package pl.coolture.restapi.common.exceptions;

/**
 * Thrown when an authenticated user attempts an action they are not permitted to perform
 * Maps to HTTP 403 Forbidden.
 */
public class ForbiddenException extends DomainException {

  private static final int HTTP_STATUS = 403;

  public ForbiddenException(String message) {
    super(message, HTTP_STATUS);
  }
}