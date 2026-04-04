package pl.coolture.restapi.media;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import pl.coolture.restapi.common.config.security.SecurityUtils;

@Entity
@Table(name = "images")
@Getter
public class ImageEntity {
  @Id
  @Column(name = "image_id")
  private UUID id;

  @Column(name = "owner_id", nullable = false, updatable = false)
  private UUID ownerId;

  @Column(name = "s3_key", nullable = false, updatable = false)
  private String s3Key;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  protected ImageEntity() {}

  public ImageEntity(String s3Key) {
    this.id = UUID.randomUUID();
    this.ownerId = UUID.fromString(SecurityUtils.getCurrentUserId());
    this.s3Key = s3Key;
    this.createdAt = LocalDateTime.now();
  }
}
