package pl.coolture.restapi.comment.api.dto;

import java.time.Instant;
import java.util.UUID;
import pl.coolture.restapi.user.api.dto.UserSummaryDto;

/**
 * `depth` is derived from ancestor_ids length, not stored in the DB.
 */
public record CommentSummaryDto(
        UUID            id,
        UUID            postId,
        UUID            rootCommentId,
        UUID            parentCommentId,
        UserSummaryDto  author,
        String          content,
        int             depth,
        int             repliesCount,
        Instant         createdAt,
        Instant         lastEditedAt,
        Instant         deletedAt,
        String          status
) {}