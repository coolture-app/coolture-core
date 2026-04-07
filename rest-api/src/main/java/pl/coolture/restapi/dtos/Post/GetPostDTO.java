package pl.coolture.restapi.dtos.Post;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import pl.coolture.restapi.models.Post;

@Data
@Builder
public class GetPostDTO {
  private UUID id;
  private String title;
  private UserMinDTO user;
  private LocalDateTime dateOfEvent;
  private LocalDateTime dateOfPosting;
  private EventLocationDTO location;
  private String description;
  private List<String> photos;
  private int likesCount;
  private int participatingCount;
  private int commentCount;
  private List<PostCommentDTO> comments;

  public static GetPostDTO fromEntity(Post post) {
    return GetPostDTO.builder()
        .id(post.getUuid())
        .title(post.getTitle())
        .user(UserMinDTO.fromEntity(post.getAuthor()))
        .dateOfEvent(post.getDateOfEvent())
        .dateOfPosting(post.getDateOfPosting())
        .location(EventLocationDTO.fromEntity(post.getLocation()))
        .description(post.getDescription())
        .photos(
            post.getPhotosUUID() != null
                ? post.getPhotosUUID().stream().map(UUID::toString).collect(Collectors.toList())
                : null)
        .likesCount(post.getLikesCount())
        .participatingCount(post.getParticipatingCount())
        .commentCount(post.getComments() != null ? post.getComments().size() : 0)
        .comments(
            post.getComments() != null
                ? post.getComments().stream()
                    .filter(comment -> comment.getParentComment() == null)
                    .map(PostCommentDTO::fromEntity)
                    .collect(Collectors.toList())
                : List.of())
        .build();
  }
}
