package pl.coolture.restapi.comment.api;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.coolture.restapi.comment.api.dto.CommentCreateRequest;
import pl.coolture.restapi.comment.api.dto.CommentSummaryDto;
import pl.coolture.restapi.comment.api.dto.CommentUpdateRequest;
import pl.coolture.restapi.comment.application.CommentService;
import pl.coolture.restapi.common.config.security.SecurityUtils;
import pl.coolture.restapi.common.pagination.CursorPage;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/posts/{postId}/comments")
    public CursorPage<CommentSummaryDto> list(
            @PathVariable UUID postId,
            @RequestParam(required = false) UUID parentCommentId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        return commentService.list(postId, parentCommentId, cursor, limit);
    }

    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<CommentSummaryDto> create(
            @PathVariable UUID postId,
            @Valid @RequestBody CommentCreateRequest request) {
        UUID callerId = UUID.fromString(SecurityUtils.getCurrentUserId());
        CommentSummaryDto created = commentService.create(postId, callerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/comments/{commentId}")
    public CommentSummaryDto update(
            @PathVariable UUID commentId,
            @Valid @RequestBody CommentUpdateRequest request) {
        UUID callerId = UUID.fromString(SecurityUtils.getCurrentUserId());
        return commentService.update(commentId, callerId, request);
    }

    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID commentId) {
        UUID callerId = UUID.fromString(SecurityUtils.getCurrentUserId());
        commentService.softDelete(commentId, callerId);
    }
}