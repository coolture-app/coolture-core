package pl.coolture.restapi.media.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfileImageRepository extends JpaRepository<ProfileImage, UUID> {

    Optional<ProfileImage> findByUserIdAndIsActiveTrue(UUID userId);

    /** Deactivates all active profile images for a user in a single UPDATE. */
    @Modifying
    @Query("UPDATE ProfileImage p SET p.isActive = false, p.unsetAt = :now WHERE p.userId = :userId AND p.isActive = true")
    void deactivateAllForUser(@Param("userId") UUID userId, @Param("now") Instant now);
}