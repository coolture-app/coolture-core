package pl.coolture.gateway.config;

import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;
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

    @Value("${app.allowed-origins:http://localhost:3900,http://localhost:8080,http://localhost:8180,http://localhost:8081,http://localhost:4200}")
    private String allowedOrigins;

    @Value("${app.frontend.redirect-uri:http://localhost:4200/}")
    private String frontendRedirectUri;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http.authorizeHttpRequests(
                auth -> auth
                        .requestMatchers(
                                "/actuator/**",
                                "/" + bucketName + "/**",
                                "/api/swagger-ui.html",
                                "/api/swagger-ui/**",
                                "/api/v3/api-docs/**",
                                "/api/api-docs/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/posts").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .oauth2Login(oauth2 -> oauth2
                        .successHandler((request, response, authentication) -> {
                            response.sendRedirect(frontendRedirectUri);
                        }))
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
                        }));
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(
                Arrays.stream(allowedOrigins.split(","))
                        .map(String::trim)
                        .collect(Collectors.toList()));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}