package pl.coolture.restapi.comment.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CommentCreateRequest(
        @NotBlank
        @Size(max = 512)
        String content,

        /** Omit / null for a root comment, parent ID to reply. */
        UUID parentCommentId
) {}