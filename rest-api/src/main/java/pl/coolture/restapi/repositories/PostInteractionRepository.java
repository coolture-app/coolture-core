package pl.coolture.restapi.repositories;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.coolture.restapi.models.PostInteraction;

public interface PostInteractionRepository extends JpaRepository<PostInteraction, UUID> {
  Optional<PostInteraction> findByUserIdAndPostUuid(UUID userUuid, UUID postUuid);
}
