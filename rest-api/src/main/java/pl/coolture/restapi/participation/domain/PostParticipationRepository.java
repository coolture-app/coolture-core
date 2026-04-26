package pl.coolture.restapi.participation.domain;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostParticipationRepository
        extends JpaRepository<PostParticipation, PostParticipationId> {

    Optional<PostParticipation> findById(PostParticipationId id);

    @Query("SELECT p FROM PostParticipation p WHERE p.id.userId = :userId AND p.id.postId IN :postIds")
    List<PostParticipation> findByUserIdAndPostIds(
            @Param("userId")  UUID userId,
            @Param("postIds") List<UUID> postIds);

    default Map<UUID, String> participationTypeByPostId(UUID userId, List<UUID> postIds) {
        return findByUserIdAndPostIds(userId, postIds).stream()
                .collect(Collectors.toMap(p -> p.getId().getPostId(), PostParticipation::getType));
    }
}
