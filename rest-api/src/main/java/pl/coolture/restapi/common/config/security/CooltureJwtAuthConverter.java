package pl.coolture.restapi.common.config.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class CooltureJwtAuthConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

  @Value("${env.keycloak.client-id}")
  private String keycloakClientId;

  @Override
  public Collection<GrantedAuthority> convert(Jwt source) {
    // Extract client roles from the "resource_access" claim
    List<String> clientRoles =
        Optional.ofNullable(source.getClaimAsMap("resource_access"))
            .map(map -> map.get(keycloakClientId))
            .filter(Map.class::isInstance)
            .map(map -> ((Map<?, ?>) map).get("roles"))
            .filter(List.class::isInstance)
            // cast each element to String
            .map(
                list ->
                    ((List<?>) list)
                        .stream()
                            .filter(String.class::isInstance)
                            .map(String.class::cast)
                            .collect(Collectors.toList()))
            // default to empty list if not found
            .orElse(List.of());

    // Convert role names to Spring Security authorities with "ROLE_" prefix if missing
    return clientRoles.stream()
        .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toList());
  }
}
