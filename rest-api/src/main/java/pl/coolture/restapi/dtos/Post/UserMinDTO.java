package pl.coolture.restapi.dtos.Post;

import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import pl.coolture.restapi.models.User;

@Data
@Builder
public class UserMinDTO {
  private UUID id;
  private String username;
  private String avatarUrl;
  private boolean isVerified;

  public static UserMinDTO fromEntity(User user) {
    return UserMinDTO.builder()
        .id(user.getId())
        .username(user.getUsername())
        .avatarUrl(user.getAvatarUrl())
        .build();
  }
}
