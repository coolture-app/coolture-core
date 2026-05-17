package pl.coolture.restapi.embedding.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "user_embeddings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEmbedding {
  @Id
  @Column(updatable = false, nullable = false)
  private UUID userId;

  @JdbcTypeCode(SqlTypes.VECTOR)
  @Column(nullable = false, columnDefinition = "vector(1536)")
  private float[] embedding;

  @Column(nullable = false, length = 128)
  private String model;

  @Column(nullable = false)
  private Instant updatedAt;
}
