package pl.coolture.restapi.reaction.domain;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostReactionRepository extends JpaRepository<PostReaction, PostReactionId> {

    Optional<PostReaction> findById(PostReactionId id);

    /** Batch lookup of the caller's reactions for a list of posts - to load on feed load */
    @Query("SELECT r FROM PostReaction r WHERE r.id.userId = :userId AND r.id.postId IN :postIds")
    List<PostReaction> findByUserIdAndPostIds(
            @Param("userId")  UUID userId,
            @Param("postIds") List<UUID> postIds);

    /** Returns a map of postId <-> reactionType for the given caller and post IDs. */
    default Map<UUID, ReactionType> reactionTypeByPostId(UUID userId, List<UUID> postIds) {
        return findByUserIdAndPostIds(userId, postIds).stream()
                .collect(Collectors.toMap(
                        r -> r.getId().getPostId(),
                        r -> ReactionType.from(r.getType())));
    }
}