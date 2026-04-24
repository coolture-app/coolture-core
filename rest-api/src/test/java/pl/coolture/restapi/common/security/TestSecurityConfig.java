package pl.coolture.restapi.common.security;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * Replaces the real JwtDecoder in @WebMvcTest
 *
 * Without this, OAuth2ResourceServerAutoConfiguration tries to fetch the JWK
 * set from Keycloak at context startup, which fails in tests.
 * This config lets the context start while keeping the real SecurityConfig
 * (and its permitAll / authenticated rules) fully active.
 *
 * For tests that need to simulate an authenticated user, use
 * MockMvcRequestPostProcessors.jwt() from spring-security-test:
 * mockMvc.perform(get("/posts").with(jwt()))
 *
 * To simulate a request with specific roles:
 * mockMvc.perform(get("/admin").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_COOLTURE_ADMIN"))))
 *
 * That post-processor bypasses the decoder entirely and injects a
 * pre-built JwtAuthenticationToken directly into the security context.
 */
@TestConfiguration
public class TestSecurityConfig {

    @Bean
    @Primary
    public JwtDecoder jwtDecoder() {
        // Returns a decoder that always rejects tokens.
        // Real decoding is never needed in unit tests
        // jwt() post-processor short-circuits the filter chain before the decoder is called.
        return token -> {
            throw new org.springframework.security.oauth2.jwt.JwtException(
                    "Test context: use MockMvcRequestPostProcessors.jwt() to simulate auth");
        };
    }
}