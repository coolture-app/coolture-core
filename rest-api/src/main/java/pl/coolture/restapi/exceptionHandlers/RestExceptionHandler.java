package pl.coolture.restapi.exceptionHandlers;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pl.coolture.restapi.exceptionHandlers.exceptionTypes.ApiException;

@RestControllerAdvice
public class RestExceptionHandler {

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ApiError> handleException(ApiException ex, HttpServletRequest request) {
    ApiError err =
        new ApiError(
            ex.getStatusCode(), ex.getErrorTitle(), ex.getMessage(), request.getRequestURI());
    return ResponseEntity.status(ex.getStatusCode()).body(err);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleGeneralException(Exception ex, HttpServletRequest request) {
    ApiError err =
        new ApiError(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal Server Error",
            ex.getMessage(),
            request.getRequestURI());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
  }
}
