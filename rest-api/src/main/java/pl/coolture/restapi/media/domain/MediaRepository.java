package pl.coolture.restapi.media.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaRepository extends JpaRepository<Media, UUID> {

    Optional<Media> findByIdAndOwnerId(UUID id, UUID ownerId);
}