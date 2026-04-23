package pl.coolture.restapi.common.exceptions;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

/**
 * RFC 7807 Problem Details for HTTP APIs response body
 * Serialised as application/problem+json
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProblemDetails {

    // URI identifying the problem type
    private final String type;
    // summary of the problem type
    private final String title;
    // HTTP status code
    private final int status;
    // explanation specific to occurrence
    private final String detail;
    // URI reference identifying the specific occurrence (optional)
    private final String instance;
    // correlation ID taken from request - required by OpenAPI contract
    private final String traceId;

    // only present on 400 responses
    // field name -> list of violation messages; null when not a validation error
    private final Map<String, List<String>> errors;
}