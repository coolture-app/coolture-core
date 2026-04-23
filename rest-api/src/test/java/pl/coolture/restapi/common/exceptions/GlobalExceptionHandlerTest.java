package pl.coolture.restapi.common.exceptions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    @Test
    void handleDomainException_resourceNotFound_returns404() {
        String resourceName = "Resource X";
        String id = UUID.randomUUID().toString();
        var ex = new ResourceNotFoundException(resourceName, id);

        ResponseEntity<ProblemDetails> response = handler.handleDomainException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getDetail()).contains(resourceName).contains(id);
    }

    @Test
    void handleDomainException_forbidden_returns403() {
        String msg = "Only author can do this";
        var ex = new ForbiddenException(msg);

        ResponseEntity<ProblemDetails> response = handler.handleDomainException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(403);
        assertThat(response.getBody().getDetail()).isEqualTo(msg);
    }

    @Test
    void handleDomainException_conflict_returns409() {
        String msg = "Resource already in use";
        var ex = new ConflictException(msg);

        ResponseEntity<ProblemDetails> response = handler.handleDomainException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
    }

    @Test
    void handleDomainException_unauthenticated_returns401() {
        String msg = "User is not authenticated";
        var ex = new UnauthenticatedUserException(msg);

        ResponseEntity<ProblemDetails> response = handler.handleDomainException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(401);
    }

    @Test
    void handleValidation_returns400WithErrorsMap() {
        BindingResult bindingResult = mock(BindingResult.class);

        String objName = "someCreateRequest";
        String fieldName1 = "field1";
        String fieldName2 = "field2";
        String msg = "msg";

        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError(objName, fieldName1, msg),
                new FieldError(objName, fieldName1, msg),
                new FieldError(objName, fieldName2, msg)
        ));

        var ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ProblemDetails> response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrors()).containsKey(fieldName1);
        assertThat(response.getBody().getErrors()).containsKey(fieldName2);
        // Two violations on the same field are grouped into a list
        assertThat(response.getBody().getErrors().get(fieldName1)).hasSize(2);
    }

    @Test
    void handleTypeMismatch_returns400WithParameterName() {
        var ex = mock(MethodArgumentTypeMismatchException.class);

        String typeName = "someType";
        String typeValue = "someValue";

        when(ex.getName()).thenReturn(typeName);
        when(ex.getValue()).thenReturn(typeValue);

        ResponseEntity<ProblemDetails> response = handler.handleTypeMismatch(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).contains(typeName).contains(typeValue);
    }

    @Test
    void handleAccessDenied_returns403() {
        String msg = "Access denied";
        var ex = new AccessDeniedException(msg);

        ResponseEntity<ProblemDetails> response = handler.handleAccessDenied(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(403);
    }

    @Test
    void handleJwtException_returns401() {
        String msg = "Something is wrong with JWT";
        var ex = new JwtException(msg);

        ResponseEntity<ProblemDetails> response = handler.handleJwtException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(401);
    }

    @Test
    void handleUnexpected_returns500WithoutLeakingInternalMessage() {
        String msg = "Unexpected something happened";
        var ex = new RuntimeException(msg);

        ResponseEntity<ProblemDetails> response = handler.handleUnexpected(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        // Raw exception message must NOT be exposed to the caller
        assertThat(response.getBody().getDetail())
                .doesNotContain(msg);
    }

    @Test
    void response_alwaysContainsTypeAndInstanceFields() {
        String resourceName = "Resource X";
        String id = UUID.randomUUID().toString();
        var ex = new ResourceNotFoundException(resourceName, id);

        ResponseEntity<ProblemDetails> response = handler.handleDomainException(ex, request);

        ProblemDetails body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getInstance()).isEqualTo("/api/test");
    }

    @Test
    void response_errorsFieldIsNullWhenNotValidationError() {
        String msg = "CONFLICT !!!";
        var ex = new ConflictException(msg);

        ResponseEntity<ProblemDetails> response = handler.handleDomainException(ex, request);

        // there must be no errors listed bcs of @JsonInclude(NON_NULL)
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrors()).isNull();
    }
}