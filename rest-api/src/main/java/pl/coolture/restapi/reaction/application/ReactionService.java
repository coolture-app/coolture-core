package pl.coolture.restapi.reaction.application;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.coolture.restapi.common.exceptions.ResourceNotFoundException;
import pl.coolture.restapi.post.domain.Post;
import pl.coolture.restapi.post.domain.PostRepository;
import pl.coolture.restapi.reaction.api.dto.ReactionRequest;
import pl.coolture.restapi.reaction.domain.PostReaction;
import pl.coolture.restapi.reaction.domain.PostReactionId;
import pl.coolture.restapi.reaction.domain.PostReactionRepository;
import pl.coolture.restapi.reaction.domain.ReactionType;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReactionService {

    private final PostReactionRepository reactionRepository;
    private final PostRepository         postRepository;

    /**
     * Upsert the caller reaction on a post.
     *
     * Cases:
     *   - No prior reaction  - insert row, bump matching counter
     *   - Same type again    - idempotent PUT
     *   - Different type     - update type, swap counters.
     */
    @Transactional
    public void upsert(UUID postId, UUID callerId, ReactionRequest request) {
        Post post = findActiveOrThrow(postId);
        PostReactionId pk = new PostReactionId(callerId, postId);
        Optional<PostReaction> existing = reactionRepository.findById(pk);

        if (existing.isPresent()) {
            ReactionType oldType = ReactionType.from(existing.get().getType());
            if (oldType == request.type()) {
                return;
            }
            decrementCounter(post, oldType);
            incrementCounter(post, request.type());
            existing.get().setType(request.type().getValue());
        } else {
            reactionRepository.save(PostReaction.builder()
                    .id(pk)
                    .type(request.type().getValue())
                    .createdAt(Instant.now())
                    .build());
            incrementCounter(post, request.type());
        }
    }

    @Transactional
    public void delete(UUID postId, UUID callerId) {
        Post post = findActiveOrThrow(postId);
        PostReactionId pk = new PostReactionId(callerId, postId);

        reactionRepository.findById(pk).ifPresent(reaction -> {
            decrementCounter(post, ReactionType.from(reaction.getType()));
            reactionRepository.delete(reaction);
        });
    }

    public Map<UUID, ReactionType> findReactionTypesForPosts(UUID callerId, List<UUID> postIds) {
        if (postIds.isEmpty()) return Map.of();
        return reactionRepository.findByUserIdAndPostIds(callerId, postIds).stream()
                .collect(Collectors.toMap(
                        r -> r.getId().getPostId(),
                        r -> ReactionType.from(r.getType())));
    }

    private static void incrementCounter(Post post, ReactionType type) {
        switch (type) {
            case LIKE    -> post.setPositiveReactionCount(post.getPositiveReactionCount() + 1);
            case DISLIKE -> post.setNegativeReactionCount(post.getNegativeReactionCount() + 1);
        }
    }

    private static void decrementCounter(Post post, ReactionType type) {
        switch (type) {
            case LIKE    -> { if (post.getPositiveReactionCount() > 0)
                post.setPositiveReactionCount(post.getPositiveReactionCount() - 1); }
            case DISLIKE -> { if (post.getNegativeReactionCount() > 0)
                post.setNegativeReactionCount(post.getNegativeReactionCount() - 1); }
        }
    }

    private Post findActiveOrThrow(UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", postId));
        if ("DELETED".equals(post.getStatus()) || post.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Post", postId);
        }
        return post;
    }
}