package pl.coolture.restapi.media.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "profile_images")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID fullMediaId;

    @Column(nullable = false)
    private UUID thumbnailMediaId;

    /** Only one row per user should have isActive = true at any time. */
    @Column(nullable = false)
    private boolean isActive;

    @Column(nullable = false)
    private Instant setAt;

    /** Filled when a newer image replaces this one (isActive becomes false). */
    private Instant unsetAt;
}