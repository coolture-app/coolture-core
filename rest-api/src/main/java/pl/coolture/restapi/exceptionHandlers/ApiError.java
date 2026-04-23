package pl.coolture.restapi.exceptionHandlers;

import java.time.Instant;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiError {
  HttpStatus statusCode;
  String error;
  String message;
  Instant timestamp;
  String path;

  public ApiError(HttpStatus status, String error, String message, String path) {
    this.statusCode = status;
    this.error = error;
    this.message = message;
    this.timestamp = Instant.now();
    this.path = path;
  }
}
