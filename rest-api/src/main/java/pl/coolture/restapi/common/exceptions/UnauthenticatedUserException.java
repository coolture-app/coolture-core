package pl.coolture.restapi.common.exceptions;

/**
 * Thrown when an operation requires an authenticated user but no valid
 * authentication context is present in the SecurityContextHolder
 *
 * Spring blocks unauthenticated requests at the filter level for protected routes.
 * This exception covers cases where SecurityUtils.getCurrentUserId()
 * is called in a code path that is reachable only after authentication
 * but the JWT is not valid
 *
 * Maps to HTTP 401 Unauthorized
 */
public class UnauthenticatedUserException extends DomainException {

  private static final int HTTP_STATUS = 401;

  public UnauthenticatedUserException(String message) {
    super(message, HTTP_STATUS);
  }
}