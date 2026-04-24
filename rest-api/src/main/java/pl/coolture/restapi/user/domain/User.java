package pl.coolture.restapi.user.domain;

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
import org.hibernate.annotations.Formula;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

  /**
   * UUID taken directly from the Keycloak JWT "sub" claim.
   * No @GeneratedValue then
   */
  @Id
  @Column(updatable = false, nullable = false)
  private UUID id;

  @Column(nullable = false, unique = true, length = 32)
  private String username;

  @Column(nullable = false, length = 128)
  private String firstName;

  @Column(nullable = false, length = 128)
  private String lastName;

  @Column(length = 512)
  private String bio;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  /**
   * TODO: uncomment it when relations are added
   * Subquery-based counters avoid denormalised columns on the users table
   * while keeping User projections simple.
   * These become correct automatically once UserRelation rows exist
   */
//  @Formula("(SELECT COUNT(*) FROM user_relations ur " +
//          "WHERE ur.target_user_id = id AND ur.type = 'FOLLOW')")
//  private int followersCount;
//
//  @Formula("(SELECT COUNT(*) FROM user_relations ur " +
//          "WHERE ur.source_user_id = id AND ur.type = 'FOLLOW')")
//  private int followingCount;
}