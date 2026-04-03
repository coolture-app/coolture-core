package pl.coolture.restapi.common.config.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI customOpenAPI() {
    final String keycloakUrl = "http://localhost:8180/realms/coolture-dev/protocol/openid-connect/";

    return new OpenAPI()
        .servers(List.of(
                new Server().url("http://localhost:8081/api").description("Direct REST API (Resource Server)"),
                new Server().url("http://localhost:8080/api").description("Via Spring Cloud Gateway (BFF)")
        ))
        .components(
            new Components()
                .addSecuritySchemes(
                    "keycloak",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.OAUTH2)
                        .description("OAuth2 flow with Keycloak")
                        .flows(
                            new OAuthFlows()
                                .authorizationCode(
                                    new OAuthFlow()
                                        .authorizationUrl(keycloakUrl + "auth")
                                        .tokenUrl(keycloakUrl + "token")
                                        .scopes(
                                            new Scopes()
                                                .addString("openid", "OpenID")
                                                .addString("profile", "Profile information")
                                                .addString("email", "Email address")
                                                .addString("roles", "User roles"))))))
        .addSecurityItem(new SecurityRequirement().addList("keycloak"));
  }
}
