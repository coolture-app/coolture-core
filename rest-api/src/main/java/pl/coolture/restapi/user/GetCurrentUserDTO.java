package pl.coolture.restapi.user;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GetCurrentUserDTO {
  String username;
  String email;
  String fullName;
  List<String> roles;
  String userId;
}
