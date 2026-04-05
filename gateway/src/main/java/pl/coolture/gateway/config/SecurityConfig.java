package pl.coolture.gateway.config;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Configuration
public class SecurityConfig {
  @Value("${GARAGE_BUCKET_NAME:coolture-bucket}")
  private String bucketName;

  @Value("${COOLTURE_PUBLIC_HOST:localhost}")
  private String publicHost;

  /** When TLS terminates in front of nginx, allow https:// origins for CORS (SPA + Keycloak iframes). */
  @Value("${COOLTURE_PUBLIC_SCHEME:http}")
  private String publicScheme;

  @Value("${COOLTURE_FRONTEND_PORT:4000}")
  private int frontendPort;

  /** Published host port for the gateway (e.g. 80 with compose mapping 80:8080). */
  @Value("${GATEWAY_HOST_PORT:8080}")
  private int gatewayHostPort;

  @Value("${KEYCLOAK_PORT:8180}")
  private int keycloakPort;

  @Value("${REST_API_HOST_PORT:8081}")
  private int restApiHostPort;

  @Value("${GARAGE_API_HOST_PORT:3900}")
  private int garageApiHostPort;

  @Value("${COOLTURE_NG_SERVE_PORT:4200}")
  private int ngServePort;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
                    auth ->
                            auth.requestMatchers("/actuator/**", "/" + bucketName + "/**")
                                    .permitAll()
                                    .requestMatchers("/login", "/login/**")
                                    .permitAll()
                                    .requestMatchers(HttpMethod.GET, "/api/posts/**", "/api/posts")
                                    .permitAll()
                                    .requestMatchers("/api/**").authenticated()
                                    .anyRequest()
                                    .authenticated())
            .oauth2Login(oauth2 -> oauth2
                    .successHandler(
                        (request, response, authentication) ->
                            response.sendRedirect(oauthSuccessUrl((HttpServletRequest) request))))
            .oauth2Client(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .exceptionHandling(exceptions -> exceptions
                    .authenticationEntryPoint((request, response, authException) -> {
                      if (request.getRequestURI().startsWith("/api/")) {
                        response.setStatus(HttpStatus.UNAUTHORIZED.value());
                      } else {
                        response.sendRedirect("/oauth2/authorization/keycloak");
                      }
                    })
            );

    return http.build();
  }

  private String oauthSuccessUrl(HttpServletRequest request) {
    return ServletUriComponentsBuilder.fromRequest(request)
        .port(frontendPort)
        .replacePath("/")
        .replaceQuery(null)
        .fragment(null)
        .build()
        .toUriString();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowCredentials(true);
    config.setAllowedOrigins(corsAllowedOrigins());
    config.setAllowedHeaders(List.of("*"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }

  private List<String> corsAllowedOrigins() {
    List<String> origins = new ArrayList<>();
    addHttpOrigin(origins, publicHost, garageApiHostPort);
    addHttpOrigin(origins, publicHost, gatewayHostPort);
    addHttpOrigin(origins, publicHost, keycloakPort);
    addHttpOrigin(origins, publicHost, restApiHostPort);
    addHttpOrigin(origins, publicHost, frontendPort);
    addHttpOrigin(origins, publicHost, ngServePort);
    if ("https".equalsIgnoreCase(publicScheme)) {
      addHttpsOrigin(origins, publicHost, garageApiHostPort);
      addHttpsOrigin(origins, publicHost, gatewayHostPort);
      addHttpsOrigin(origins, publicHost, keycloakPort);
      addHttpsOrigin(origins, publicHost, restApiHostPort);
      addHttpsOrigin(origins, publicHost, frontendPort);
      addHttpsOrigin(origins, publicHost, ngServePort);
    }
    return origins;
  }

  /**
   * Browsers often send {@code Origin: http://host} without {@code :80}; allow both when the gateway uses port 80.
   */
  private static void addHttpOrigin(List<String> origins, String host, int port) {
    String withPort = "http://" + host + ":" + port;
    if (!origins.contains(withPort)) {
      origins.add(withPort);
    }
    if (port == 80) {
      String defaultHttp = "http://" + host;
      if (!origins.contains(defaultHttp)) {
        origins.add(defaultHttp);
      }
    }
  }

  /**
   * Mirrors {@link #addHttpOrigin} for TLS. Port 80 here means “public site on default HTTPS” (no :port in browser URL).
   */
  private static void addHttpsOrigin(List<String> origins, String host, int port) {
    if (port == 80) {
      String defaultHttps = "https://" + host;
      if (!origins.contains(defaultHttps)) {
        origins.add(defaultHttps);
      }
      return;
    }
    String withPort = "https://" + host + ":" + port;
    if (!origins.contains(withPort)) {
      origins.add(withPort);
    }
    if (port == 443) {
      String defaultHttps = "https://" + host;
      if (!origins.contains(defaultHttps)) {
        origins.add(defaultHttps);
      }
    }
  }
}
