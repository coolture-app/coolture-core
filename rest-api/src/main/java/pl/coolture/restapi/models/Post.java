package pl.coolture.restapi.models;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import pl.coolture.restapi.user.domain.User;
import pl.coolture.restapi.utils.UuidListConverter;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "posts")
public class Post {
  @Id @GeneratedValue private UUID uuid;

  @Column(nullable = false)
  private String title;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "authorUUID")
  private User author;

  @Column(nullable = false)
  private LocalDateTime dateOfEvent;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private LocalDateTime dateOfPosting;

  @Column(nullable = false)
  private String description;

  @Convert(converter = UuidListConverter.class)
  @Column(columnDefinition = "TEXT", nullable = false)
  private List<UUID> photosUUID;

  @Column(nullable = false)
  private int likesCount;

  @Column(nullable = false)
  private int participatingCount;

  @OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
  private List<PostComment> comments = new ArrayList<>();
}
