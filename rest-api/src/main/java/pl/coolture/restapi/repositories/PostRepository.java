package pl.coolture.restapi.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.coolture.restapi.models.Location;
import pl.coolture.restapi.models.Post;

public interface PostRepository extends JpaRepository<Post, UUID> {
  List<Post> findAllByAuthorId(UUID authorId);

  @Query(
      "SELECT p FROM Post p WHERE "
          + "LOWER(p.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
          + "LOWER(p.title) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
  List<Post> searchPosts(@Param("searchTerm") String searchTerm);

  List<Post> findAllByOrderByDateOfPostingDesc();

  List<Post> findAllByLocation(Location location);

  @Query(
      "SELECT DISTINCT p FROM Post p "
          + "LEFT JOIN FETCH p.author "
          + "LEFT JOIN FETCH p.comments c "
          + "LEFT JOIN FETCH c.author "
          + "WHERE p.uuid = :uuid "
          + "AND (c IS NULL OR c.parentComment IS NULL)")
  Optional<Post> findPostWithAuthorAndMainComments(@Param("uuid") UUID uuid);

  @Modifying
  @Query("UPDATE Post p SET p.likesCount = p.likesCount + 1 WHERE p.id = :postId")
  void incrementLikes(@Param("postId") UUID postId);

  @Modifying
  @Query("UPDATE Post p SET p.likesCount = p.likesCount - 1 WHERE p.id = :postId")
  void decrementLikes(@Param("postId") UUID postId);

  @Modifying
  @Query("UPDATE Post p SET p.participatingCount = p.participatingCount + 1 WHERE p.id = :postId")
  void incrementParticipating(@Param("postId") UUID postId);

  @Modifying
  @Query("UPDATE Post p SET p.participatingCount = p.participatingCount - 1 WHERE p.id = :postId")
  void decrementParticipating(@Param("postId") UUID postId);
}
