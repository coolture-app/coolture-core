package pl.coolture.restapi.user.application;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.coolture.restapi.user.domain.User;
import pl.coolture.restapi.user.domain.UserRepository;

/**
 * Called by UserProvisioningFilter on every authenticated request.
 * Creates a local user entry in DB the first time a Keycloak subject is seen.
 *
 * Identity data stays in Keycloak
 * only the profile fields needed by the app are mirrored here.
 *
 * firstName / lastName are synced from the JWT on every provisioning call
 * so that Keycloak admin changes are reflected without requiring the user
 * to manually update their profile.
 * Username is synced only on creation; users own it after that.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProvisioningService {

    private final UserRepository userRepository;

    @Transactional
    public void ensureExists(Jwt jwt) {
        UUID id = UUID.fromString(jwt.getSubject());

        userRepository.findById(id).ifPresentOrElse(
                existing -> syncFromJwt(existing, jwt),
                () -> createFromJwt(id, jwt));
    }

    private void createFromJwt(UUID id, Jwt jwt) {
        User user = User.builder()
                .id(id)
                .username(claim(jwt,"preferred_username", id.toString()))
                .firstName(claim(jwt,"given_name",""))
                .lastName(claim(jwt, "family_name",""))
                .createdAt(Instant.now())
                .build();

        userRepository.save(user);
        log.info("Provisioned new user id={} username={}", id, user.getUsername());
    }

    /** Keeps first/last name in sync with Keycloak without overwriting user-managed fields. */
    private void syncFromJwt(User user, Jwt jwt) {
        String firstName = claim(jwt, "given_name", user.getFirstName());
        String lastName  = claim(jwt, "family_name", user.getLastName());

        if (!firstName.equals(user.getFirstName()) || !lastName.equals(user.getLastName())) {
            user.setFirstName(firstName);
            user.setLastName(lastName);
        }
    }

    private String claim(Jwt jwt, String claim, String fallback) {
        String value = jwt.getClaimAsString(claim);
        return (value != null && !value.isBlank()) ? value : fallback;
    }
}