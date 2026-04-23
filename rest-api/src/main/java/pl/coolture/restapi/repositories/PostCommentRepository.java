package pl.coolture.restapi.repositories;


import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.coolture.restapi.models.Post;
import pl.coolture.restapi.models.PostComment;

public interface PostCommentRepository extends JpaRepository<PostComment, UUID> {
  List<PostComment> findAllByPost(Post post);

  Optional<PostComment> findByUuid(UUID postUuid);
}
