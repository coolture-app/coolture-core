package pl.coolture.restapi.media.api;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.coolture.restapi.common.exceptions.ForbiddenException;
import pl.coolture.restapi.media.api.dto.ProfileImageResponse;
import pl.coolture.restapi.media.api.dto.SetProfileImageRequest;
import pl.coolture.restapi.media.application.ProfileImageService;

@RestController
@RequestMapping("/users/{userId}/profile-image")
@RequiredArgsConstructor
public class ProfileImageController {

    private final ProfileImageService profileImageService;

    @PutMapping
    public ResponseEntity<ProfileImageResponse> set(
            @PathVariable UUID userId,
            @Valid @RequestBody SetProfileImageRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        requireSelf(userId, jwt);
        return ResponseEntity.ok(profileImageService.set(userId, request));
    }

    @DeleteMapping
    public ResponseEntity<Void> remove(
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt) {

        requireSelf(userId, jwt);
        profileImageService.remove(userId);
        return ResponseEntity.noContent().build();
    }

    private void requireSelf(UUID userId, Jwt jwt) {
        if (!userId.equals(UUID.fromString(jwt.getSubject()))) {
            throw new ForbiddenException("You can only manage your own profile image");
        }
    }
}