package pl.coolture.restapi.participation.application;

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
import pl.coolture.restapi.participation.api.dto.ParticipationRequest;
import pl.coolture.restapi.participation.domain.ParticipationType;
import pl.coolture.restapi.participation.domain.PostParticipation;
import pl.coolture.restapi.participation.domain.PostParticipationId;
import pl.coolture.restapi.participation.domain.PostParticipationRepository;
import pl.coolture.restapi.post.domain.Post;
import pl.coolture.restapi.post.domain.PostRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParticipationService {

    private final PostParticipationRepository participationRepository;
    private final PostRepository              postRepository;

    /**
     * Upsert the caller participation on a post.
     *
     * Switching between `interested` and `takes_part` does not affect
     * participant_count - the counter tracks presence, not declaration type.
     */
    @Transactional
    public void upsert(UUID postId, UUID callerId, ParticipationRequest request) {
        Post post = findActiveOrThrow(postId);
        PostParticipationId pk = new PostParticipationId(callerId, postId);
        Optional<PostParticipation> existing = participationRepository.findById(pk);

        if (existing.isPresent()) {
            ParticipationType oldType = existing.get().getType();
            if (oldType == request.type()) {
                return;
            }
            existing.get().setType(request.type());
        } else {
            participationRepository.save(PostParticipation.builder()
                    .id(pk)
                    .type(request.type())
                    .createdAt(Instant.now())
                    .build());
            post.setParticipantCount(post.getParticipantCount() + 1);
        }
    }

    @Transactional
    public void delete(UUID postId, UUID callerId) {
        Post post = findActiveOrThrow(postId);
        PostParticipationId pk = new PostParticipationId(callerId, postId);

        participationRepository.findById(pk).ifPresent(p -> {
            participationRepository.delete(p);
            if (post.getParticipantCount() > 0) {
                post.setParticipantCount(post.getParticipantCount() - 1);
            }
        });
    }

    public Map<UUID, ParticipationType> findParticipationTypesForPosts(UUID callerId, List<UUID> postIds) {
        if (postIds.isEmpty()) return Map.of();
        return participationRepository.findByUserIdAndPostIds(callerId, postIds).stream()
                .collect(Collectors.toMap(
                        p -> p.getId().getPostId(),
                        p -> p.getType()));
    }

    private Post findActiveOrThrow(UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", postId));
        if (pl.coolture.restapi.post.domain.PostStatus.DELETED == post.getStatus() || post.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Post", postId);
        }
        return post;
    }
}