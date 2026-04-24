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
@Table(name = "media")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID ownerId;

    /** S3 object key: {purpose}/{ownerId}/{uuid}.{extension} */
    @Column(nullable = false, unique = true)
    private String objectKey;

    /** Original client filename stored for display purposes only. */
    @Column(nullable = false, length = 255)
    private String fileName;

    /** Maps to MediaPurpose enum values stored as strings. */
    @Column(nullable = false, length = 64)
    private String purpose;

    @Column(nullable = false, length = 64)
    private String mimeType;

    @Column(nullable = false)
    private Long sizeBytes;

    /**
     * PENDING: record created, upload URL issued, file not yet in S3
     * UPLOADED: client confirmed PUT completed; available for attachment
     * ATTACHED: linked to a post or active profile image
     * DELETED: removed from S3; record can be kept
     */
    @Column(nullable = false, length = 32)
    private String status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant deletedAt;
}