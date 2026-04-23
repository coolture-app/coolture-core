package pl.coolture.restapi.common.exceptions;

import java.util.UUID;

/**
 * Throw it when a requested resource does not exist in the system
 * Maps to HTTP 404 Not Found
 */
public class ResourceNotFoundException extends DomainException {

  private static final int HTTP_STATUS = 404;

  /**
   * @param resourceName  name of the missing resource type (e.g. "User", "Post")
   * @param id            the identifier that was looked up - toString() is called automatically
   */
  public ResourceNotFoundException(String resourceName, Object id) {
    super("%s with id '%s' was not found".formatted(resourceName, id), HTTP_STATUS);
  }

  /**
   * overload for UUID identifiers avoids casting at all calls
   */
  public ResourceNotFoundException(String resourceName, UUID id) {
    this(resourceName, (Object) id);
  }

  /**
   * Use when there is no identifier to display
   */
  public ResourceNotFoundException(String message) {
    super(message, HTTP_STATUS);
  }
}