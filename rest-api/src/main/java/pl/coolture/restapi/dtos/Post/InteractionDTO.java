package pl.coolture.restapi.dtos.Post;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pl.coolture.restapi.models.PostInteraction;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InteractionDTO {
  @NotNull(message = "User id cannot be null.")
  private UUID userUuid;

  @NotNull(message = "Post id cannot be null.")
  private UUID postUuid;

  public static InteractionDTO fromEntity(PostInteraction postInteraction) {
    return InteractionDTO.builder()
        .postUuid(postInteraction.getPost().getUuid())
        .userUuid(postInteraction.getUser().getId())
        .build();
  }
}
