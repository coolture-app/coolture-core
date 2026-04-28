package pl.coolture.restapi.common.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a request carries semantically invalid input (HTTP 400).
 * Distinct from constraint violation errors raised by Bean Validation.
 */
public class BadRequestException extends DomainException {
    private static final int HTTP_STATUS = HttpStatus.BAD_REQUEST.value();

    public BadRequestException(String message) {
        super(message, HTTP_STATUS);
    }
}