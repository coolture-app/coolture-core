package pl.coolture.restapi.comment.application;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.coolture.restapi.comment.api.CommentMapper;
import pl.coolture.restapi.comment.api.dto.CommentCreateRequest;
import pl.coolture.restapi.comment.api.dto.CommentSummaryDto;
import pl.coolture.restapi.comment.api.dto.CommentUpdateRequest;
import pl.coolture.restapi.comment.domain.Comment;
import pl.coolture.restapi.comment.domain.CommentRepository;
import pl.coolture.restapi.common.exceptions.ForbiddenException;
import pl.coolture.restapi.common.exceptions.ResourceNotFoundException;
import pl.coolture.restapi.common.pagination.CursorCodec;
import pl.coolture.restapi.common.pagination.CursorPage;
import pl.coolture.restapi.common.pagination.CursorPayload;
import pl.coolture.restapi.post.domain.Post;
import pl.coolture.restapi.post.domain.PostRepository;
import pl.coolture.restapi.user.domain.User;
import pl.coolture.restapi.user.domain.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private static final String STATUS_ACTIVE  = "ACTIVE";
    private static final String STATUS_DELETED = "DELETED";
    private static final String POST_DELETED   = "DELETED";

    /** Contract: thread depth is capped at 2 (root=0, reply=1, reply-to-reply=2). */
    private static final int MAX_DEPTH = 2;

    private final CommentRepository commentRepository;
    private final PostRepository    postRepository;
    private final UserRepository    userRepository;
    private final CommentMapper     commentMapper;
    private final CursorCodec       cursorCodec;

    /**
     * Lists either root comments (parentCommentId == null) or direct replies to a
     * specific parent. DELETED rows are kept in the page so the thread structure
     * remains intact - the client handles/renders it somehow
     */
    public CursorPage<CommentSummaryDto> list(UUID postId, UUID parentCommentId,
                                              String cursor, int limit) {
        ensurePostActive(postId);

        var payload = cursorCodec.decode(cursor);
        Instant cursorCreatedAt = payload.map(CursorPayload::createdAt).orElse(null);
        UUID    cursorId        = payload.map(CursorPayload::id).orElse(null);

        List<Comment> rows = (parentCommentId == null)
                ? commentRepository.findRootCommentsForPost(
                postId, cursorCreatedAt, cursorId, limit + 1)
                : findRepliesValidated(
                postId, parentCommentId, cursorCreatedAt, cursorId, limit + 1);

        List<CommentSummaryDto> dtos = rows.stream().map(commentMapper::toDto).toList();
        return CursorPage.of(dtos, limit,
                CommentSummaryDto::id, CommentSummaryDto::createdAt, cursorCodec);
    }

    @Transactional
    public CommentSummaryDto create(UUID postId, UUID callerId, CommentCreateRequest request) {
        Post post   = ensurePostActive(postId);
        User author = userRepository.getReferenceById(callerId);

        Comment parent = (request.parentCommentId() == null)
                ? null
                : findActiveOrThrow(request.parentCommentId());

        if (parent != null) {
            // the parent must belong to the same post as new comment
            if (!parent.getPost().getId().equals(postId)) {
                throw new ForbiddenException("Parent comment belongs to a different post");
            }
            // Block exceeding the depth cap. parent depth + 1 must stay within MAX_DEPTH.
            if (depthOf(parent) >= MAX_DEPTH) {
                throw new ForbiddenException("Maximum comment thread depth exceeded");
            }
        }

        UUID[] ancestors = buildAncestors(parent);

        Comment comment = Comment.builder()
                .post(post)
                .author(author)
                .parentComment(parent)
                // Root of the thread points at the top-most comment.
                // For first-level replies that root is the parent itself.
                .rootComment(parent == null
                        ? null
                        : (parent.getRootComment() != null ? parent.getRootComment() : parent))
                .ancestorIds(buildAncestors(parent))
                .content(request.content())
                .repliesCount(0)
                .status(STATUS_ACTIVE)
                .createdAt(Instant.now())
                .build();

        comment = commentRepository.save(comment);

        // Maintain denormalized counters
        if (ancestors.length > 0) {
            commentRepository.incrementRepliesCountForAll(Arrays.asList(ancestors));
        }
        post.setCommentsCount(post.getCommentsCount() + 1);

        return commentMapper.toDto(comment);
    }

    @Transactional
    public CommentSummaryDto update(UUID commentId, UUID callerId, CommentUpdateRequest request) {
        Comment comment = findActiveOrThrow(commentId);
        requireAuthor(comment, callerId);

        comment.setContent(request.content());
        comment.setLastEditedAt(Instant.now());
        return commentMapper.toDto(comment);
    }

    /**
     * Soft delete: clears content but preserves the db entry so existing replies are
     * not orphaned and the thread keeps its shape
     */
    @Transactional
    public void softDelete(UUID commentId, UUID callerId) {
        Comment comment = findActiveOrThrow(commentId);
        requireAuthor(comment, callerId);

        comment.setStatus(STATUS_DELETED);
        comment.setDeletedAt(Instant.now());
        comment.setContent("");

        Post post = comment.getPost();
        if (post.getCommentsCount() > 0) {
            post.setCommentsCount(post.getCommentsCount() - 1);
        }

        UUID[] ancestors =  comment.getAncestorIds();

        if (ancestors != null && ancestors.length > 0) {
            commentRepository.decrementRepliesCountForAll(Arrays.asList(ancestors));
        }
    }

    @Transactional
    public void deleteAllForPost(UUID postId) {
        commentRepository.deleteAllForPost(postId);
    }

    /**
     * For replies queries verify the parent exists and belongs to
     * the requested post
     */
    private List<Comment> findRepliesValidated(UUID postId, UUID parentId,
                                               Instant cursorCreatedAt, UUID cursorId, int limit) {
        Comment parent = commentRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", parentId));

        if (!parent.getPost().getId().equals(postId)) {
            throw new ResourceNotFoundException("Comment", parentId);
        }
        return commentRepository.findRepliesForParent(parentId, cursorCreatedAt, cursorId, limit);
    }

    private Post ensurePostActive(UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", postId));

        if (POST_DELETED.equals(post.getStatus()) || post.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Post", postId);
        }
        return post;
    }

    private Comment findActiveOrThrow(UUID commentId) {
        Comment c = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));

        if (STATUS_DELETED.equals(c.getStatus()) || c.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Comment", commentId);
        }
        return c;
    }

    private void requireAuthor(Comment comment, UUID callerId) {
        if (!comment.getAuthor().getId().equals(callerId)) {
            throw new ForbiddenException("Only the author can modify this comment");
        }
    }

    private static int depthOf(Comment c) {
        return c.getAncestorIds() == null ? 0 : c.getAncestorIds().length;
    }

    /** Returns parent.ancestorIds + parent.id, or empty array for root comments. */
    private static UUID[] buildAncestors(Comment parent) {
        if (parent == null) return new UUID[0];

        UUID[] base   = parent.getAncestorIds() != null ? parent.getAncestorIds() : new UUID[0];
        UUID[] result = Arrays.copyOf(base, base.length + 1);
        result[base.length] = parent.getId();
        return result;
    }
}