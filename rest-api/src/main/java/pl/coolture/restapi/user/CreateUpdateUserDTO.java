package pl.coolture.restapi.user;
// TODO: delete when replaced in other services
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pl.coolture.restapi.user.domain.User;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUpdateUserDTO {
  private UUID id;

  @NotBlank(message = "Username is required")
  @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
  @Pattern(
      regexp = "^[a-zA-Z0-9_]+$",
      message = "Username can only contain letters, numbers, and underscores")
  private String username;

  @NotBlank(message = "Email is required")
  @Email(message = "Email should be valid")
  private String email;

  @NotBlank(message = "First name is required")
  @Size(max = 50, message = "First name must be at most 50 characters")
  private String firstName;

  @NotBlank(message = "Last name is required")
  @Size(max = 50, message = "Last name must be at most 50 characters")
  private String lastName;

  private String bioDescription;

  private String avatarUrl;

  public static CreateUpdateUserDTO fromUser(User user) {
    return CreateUpdateUserDTO.builder()
        .id(user.getId())
        .username(user.getUsername())
//        .email(user.getEmail())
        .firstName(user.getFirstName())
        .lastName(user.getLastName())
//        .bioDescription(user.getBioDescription())
//        .avatarUrl(user.getAvatarUrl())
        .build();
  }

  public User toUser() {
    return User.builder()
        .id(this.id)
        .username(this.username)
//        .email(this.email)
        .firstName(this.firstName)
        .lastName(this.lastName)
//        .bioDescription(this.bioDescription)
//        .avatarUrl(this.avatarUrl)
        .build();
  }
}
