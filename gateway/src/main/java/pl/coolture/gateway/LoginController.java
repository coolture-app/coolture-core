package pl.coolture.gateway;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Browsers or the SPA may request {@code GET /login} (or {@code /login?error}); Spring OAuth2 also uses that path by
 * convention. Without a mapping the gateway treats it as a static file and returns 404.
 */
@RestController
public class LoginController {

  @Value("${COOLTURE_FRONTEND_PORT:4000}")
  private int frontendPort;

  @GetMapping("/login")
  public void login(
      HttpServletRequest request,
      HttpServletResponse response,
      @RequestParam(required = false) String error)
      throws IOException {
    if (error != null) {
      response.sendRedirect(
          ServletUriComponentsBuilder.fromRequest(request)
              .port(frontendPort)
              .replacePath("/")
              .queryParam("loginError", "1")
              .fragment(null)
              .build()
              .toUriString());
      return;
    }

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null
        && auth.isAuthenticated()
        && !(auth instanceof AnonymousAuthenticationToken)) {
      response.sendRedirect(
          ServletUriComponentsBuilder.fromRequest(request)
              .port(frontendPort)
              .replacePath("/")
              .fragment(null)
              .build()
              .toUriString());
      return;
    }

    response.sendRedirect(request.getContextPath() + "/oauth2/authorization/keycloak");
  }
}
