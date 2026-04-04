package pl.coolture.restapi.dtos.Post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pl.coolture.restapi.models.PostComment;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostCommentDTO {
  private UUID commentId;

  @NotNull(message = "Post UUID cannot be null")
  private UUID postUuid;

  private UUID parentUuid;

  @NotNull(message = "Author ID cannot be null")
  private UserMinDTO author;

  private Integer likesCount;

  @NotBlank(message = "Content cannot be blank")
  @Size(max = 1000, message = "Content must not exceed 1000 characters")
  private String content;

  private List<PostCommentDTO> replies;

  public static PostCommentDTO fromEntity(PostComment comment) {
    if (comment == null) {
      return null;
    }
    return PostCommentDTO.builder()
        .commentId(comment.getUuid())
        .author(UserMinDTO.fromEntity(comment.getAuthor()))
        .likesCount(comment.getLikesCount())
        .postUuid(comment.getPost().getUuid())
        .parentUuid(
            comment.getParentComment() != null ? comment.getParentComment().getUuid() : null)
        .content(comment.getContent())
        .replies(
            comment.getReplies() != null
                ? comment.getReplies().stream()
                    .map(PostCommentDTO::fromEntity)
                    .collect(Collectors.toList())
                : List.of())
        .build();
  }
}
