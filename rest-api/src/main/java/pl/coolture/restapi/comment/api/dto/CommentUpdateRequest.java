package pl.coolture.restapi.comment.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentUpdateRequest(
        @NotBlank
        @Size(max = 512)
        String content
) {}