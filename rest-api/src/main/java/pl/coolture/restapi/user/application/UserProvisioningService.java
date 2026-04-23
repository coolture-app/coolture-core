package pl.coolture.restapi.user.application;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.coolture.restapi.user.domain.User;
import pl.coolture.restapi.user.domain.UserRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserProvisioningService {
    private final UserRepository userRepository;

    @Transactional
    public User ensureExists(Jwt jwt) {
        return userRepository
                .findById(UUID.fromString(jwt.getSubject()))
                .orElseGet(() -> createUser(jwt));
    }

    private User createUser(Jwt jwt) {
        UUID id = UUID.fromString(jwt.getSubject());
        String username = jwt.getClaimAsString("username");
        String firstName = jwt.getClaimAsString("firstName");
        String lastName = jwt.getClaimAsString("lastName");
        String email = jwt.getClaimAsString("email");

        User user = User.builder()
                .id(id)
                .username(username)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .build();

        return userRepository.save(user);
    }
}
