package pl.coolture.gateway.config;

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

@Configuration
public class SecurityConfig {
  @Value("${GARAGE_BUCKET_NAME:coolture-bucket}")
  private String bucketName;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
                    auth ->
                            auth.requestMatchers("/actuator/**", "/" + bucketName + "/**")
                                    .permitAll()
                                    .requestMatchers(HttpMethod.GET, "/api/posts/**", "/api/posts")
                                    .permitAll()
                                    .requestMatchers("/api/**").authenticated()
                                    .anyRequest()
                                    .authenticated())
            .oauth2Login(oauth2 -> oauth2
                    .successHandler((request, response, authentication) -> {
                        response.sendRedirect("http://localhost:4200/");
                    })
            )
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

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowCredentials(true);
    config.setAllowedOrigins(
            List.of(
                    "http://localhost:3900", // Garage S3
                    "http://localhost:8080", // Spring Cloud Gateway
                    "http://localhost:8180", // Keycloak
                    "http://localhost:8081", // backend
                    "http://localhost:4200")); // Angular
    config.setAllowedHeaders(List.of("*"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}