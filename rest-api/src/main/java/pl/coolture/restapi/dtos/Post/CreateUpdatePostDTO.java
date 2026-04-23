package pl.coolture.restapi.dtos.Post;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pl.coolture.restapi.models.Post;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUpdatePostDTO {
  private UUID id;

  @NotBlank(message = "Title is required")
  @Size(min = 3, max = 200, message = "Title must be between 3 and 200 characters")
  @Pattern(
      regexp = "^[a-zA-Z0-9_ ]+$",
      message = "Title can only contain letters, numbers, spaces and underscores")
  private String title;

  @NotNull(message = "Author UUID is required")
  private UUID authorUuid;

  @NotNull(message = "Date of event is required")
  @Future(message = "Event cannot take place in past")
  private LocalDateTime dateOfEvent;

  @NotBlank(message = "Description is required")
  @Size(max = 2000)
  private String description;

  public static CreateUpdatePostDTO fromPost(Post post) {
    return CreateUpdatePostDTO.builder()
        .id(post.getUuid())
        .title(post.getTitle())
        .authorUuid(post.getAuthor().getId())
        .dateOfEvent(post.getDateOfEvent())
        .description(post.getDescription())
        .build();
  }
}
