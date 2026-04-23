package pl.coolture.restapi.common.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * exception → HTTP response mapper
 *
 * All responses use the application/problem+json media type as required by RFC 7807
 *
 * Mapping strategy:
 *   DomainException subclasses carry their own HTTP status.
 *   Known exceptions are mapped to status codes.
 *   Anything unexpected falls through to the 500 handler
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // URI prefix used in the "type" field
    // should point to documentation (or "#" when no docs page exists)
    private static final String PROBLEM_BASE_URI = "#";

    // MDC key that is populated by the logging framework / request filter
    private static final String TRACE_ID_MDC_KEY = "traceId";

    // Handles all DomainException subclasses
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ProblemDetails> handleDomainException(
            DomainException ex, HttpServletRequest request) {

        HttpStatus status = HttpStatus.resolve(ex.getStatus());

        // Guard against a misconfigured DomainException subclass
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        log.warn("Domain exception [{}]: {}", status.value(), ex.getMessage());

        return buildResponse(status, status.getReasonPhrase(), ex.getMessage(), request, null);
    }

    // Validation exceptions (400)

    /**
     * Handles @Valid / @Validated failures on request body DTOs
     * The "errors" map groups messages by field name so the frontend
     * can display per-field feedback without additional parsing.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetails> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, List<String>> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.groupingBy(
                        FieldError::getField,
                        Collectors.mapping(
                                // defaultMessage is the annotation message attribute
                                FieldError::getDefaultMessage,
                                Collectors.toList())));

        log.warn("Validation failed for {}: {}", request.getRequestURI(), errors);

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Validation Failed",
                "One or more request fields are invalid",
                request,
                errors);
    }

    /**
     * Handles path/query parameter type mismatches (e.g. a non-UUID string passed
     * where a UUID is expected).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetails> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        String detail = "Parameter '%s' has an invalid value: '%s'"
                .formatted(ex.getName(), ex.getValue());
        log.warn("Type mismatch on {}: {}", request.getRequestURI(), detail);

        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid Parameter", detail, request, null);
    }

    // Security exceptions

    /**
     * Spring Security throws AccessDeniedException
     * when @PreAuthorize annotations block the call.
     * Remap to 403 here so the client always sees a consistent ProblemDetails shape.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetails> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {

        log.warn("Access denied for {}: {}", request.getRequestURI(), ex.getMessage());

        return buildResponse(
                HttpStatus.FORBIDDEN,
                "Forbidden",
                "You do not have permission to perform this action",
                request,
                null);
    }

    // JWT validation errors
    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ProblemDetails> handleJwtException(
            JwtException ex, HttpServletRequest request) {

        log.warn("JWT error on {}: {}", request.getRequestURI(), ex.getMessage());

        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                "Invalid or missing authentication token",
                request,
                null);
    }

    // Catch-all (500)

    /**
     * Safety net for any exception not handled above.
     * The original message is logged but NOT sent to the client to avoid
     * leaking stack traces or internal state.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetails> handleUnexpected(
            Exception ex, HttpServletRequest request) {

        log.error("Unhandled exception on {}", request.getRequestURI(), ex);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred. Please try again later.",
                request,
                null);
    }

    // Helper

    private ResponseEntity<ProblemDetails> buildResponse(
            HttpStatus status,
            String title,
            String detail,
            HttpServletRequest request,
            Map<String, List<String>> errors) {

        ProblemDetails body = ProblemDetails.builder()
                .type(PROBLEM_BASE_URI + "/" + status.value())
                .title(title)
                .status(status.value())
                .detail(detail)
                .instance(request.getRequestURI())
                .traceId(MDC.get(TRACE_ID_MDC_KEY))
                .errors(errors)
                .build();

        return ResponseEntity
                .status(status)
                .contentType(MediaType.parseMediaType("application/problem+json"))
                .body(body);
    }
}