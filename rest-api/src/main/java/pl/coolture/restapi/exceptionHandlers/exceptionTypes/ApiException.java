package pl.coolture.restapi.exceptionHandlers.exceptionTypes;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException {
  private final HttpStatus statusCode;
  private final String errorTitle;

  public ApiException(String message, HttpStatus statusCode, String errorTitle) {
    super(message);
    this.statusCode = statusCode;
    this.errorTitle = errorTitle;
  }
}
