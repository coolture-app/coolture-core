package pl.coolture.restapi.common.config.openapi;

import org.springframework.beans.factory.annotation.Value;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
  @Value("${env.keycloak.open-id-url:http://localhost:8180/realms/coolture-dev/protocol/openid-connect/}")
  private String keycloakUrl;

  @Value("${env.host.addr:localhost}")
  private String hostAddr;

  @Value("${env.host.gateway-port:8090}")
  private String gatewayPort;

  @Value("${env.host.rest-api-port:8091}")
  private String restApiPort;

  @Bean
  public OpenAPI customOpenAPI() {

    return new OpenAPI()
        .servers(
            List.of(
                new Server()
                    .url("http://" + hostAddr + ":" + gatewayPort + "/api")
                    .description("Via Spring Cloud Gateway (BFF)")))
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
