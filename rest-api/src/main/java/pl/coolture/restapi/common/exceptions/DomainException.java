package pl.coolture.restapi.common.exceptions;

import lombok.Getter;

/**
 * Base class for all application-level domain exceptions.
 * Carries an HTTP status code so the exception handler can map it
 * to a ProblemDetails response without a if-else chain.
 */
@Getter
public abstract class DomainException extends RuntimeException {

    private final int status;

    protected DomainException(String message, int status) {
        super(message);
        this.status = status;
    }

    protected DomainException(String message, int status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }
}