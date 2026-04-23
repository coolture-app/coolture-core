package pl.coolture.restapi.exceptionHandlers.exceptionTypes;

import org.springframework.http.HttpStatus;

public class AlreadyExistsException extends ApiException {
  public AlreadyExistsException(String message) {
    super(message, HttpStatus.CONFLICT, "Conflict");
  }
}
