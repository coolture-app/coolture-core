package pl.coolture.restapi.common.config.security;

import java.util.Objects;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import pl.coolture.restapi.common.exceptions.UnauthenticatedUserException;

@Component
public class SecurityUtils {
  public static String getCurrentUserId() throws UnauthenticatedUserException, JwtException {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth == null || !auth.isAuthenticated()) {
      throw new UnauthenticatedUserException("User is not authenticated");
    }

    Jwt jwt = (Jwt) auth.getPrincipal();

    if (jwt == null) {
      throw new JwtException("JWT token is null");
    }

    return jwt.getSubject();
  }

  public static boolean isCurrentUserAdmin() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) {
      return false;
    }
    return auth.getAuthorities().stream()
        .anyMatch(granted -> Objects.equals(granted.getAuthority(), "ROLE_COOLTURE_ADMIN"));
  }
}
