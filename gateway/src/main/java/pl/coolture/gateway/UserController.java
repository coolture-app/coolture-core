package pl.coolture.gateway;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {
  @GetMapping("/username")
  public Map<String, String> username(Authentication authentication) {
    String username = authentication.getName();
    log.info("username: {}", username);
    return Map.of("username", username);
  }

  @GetMapping("/profile")
  public Map<String, Object> idToken(@AuthenticationPrincipal OidcUser oidcUser) {
    log.info("oidcUser: {}", oidcUser);
    log.info("id token: {}", oidcUser.getIdToken().getTokenValue());

    return oidcUser.getClaims();
  }
}
