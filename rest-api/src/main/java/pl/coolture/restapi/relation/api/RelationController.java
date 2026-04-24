package pl.coolture.restapi.relation.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.coolture.restapi.common.exceptions.ForbiddenException;
import pl.coolture.restapi.common.pagination.CursorPage;
import pl.coolture.restapi.relation.application.RelationService;
import pl.coolture.restapi.user.api.dto.UserSummaryDto;

@RestController
@RequestMapping("/users/{userId}")
@RequiredArgsConstructor
@Validated
public class RelationController {

    private final RelationService relationService;

    @GetMapping("/followers")
    public ResponseEntity<CursorPage<UserSummaryDto>> getFollowers(
            @PathVariable UUID userId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return ResponseEntity.ok(relationService.getFollowers(userId, cursor, limit));
    }

    @GetMapping("/following")
    public ResponseEntity<CursorPage<UserSummaryDto>> getFollowing(
            @PathVariable UUID userId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return ResponseEntity.ok(relationService.getFollowing(userId, cursor, limit));
    }

    /** Block list is private — only the authenticated user can view their own. */
    @GetMapping("/blocking")
    public ResponseEntity<CursorPage<UserSummaryDto>> getBlocking(
            @PathVariable UUID userId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
            @AuthenticationPrincipal Jwt jwt) {
        UUID callerId = UUID.fromString(jwt.getSubject());
        if (!callerId.equals(userId)) {
            throw new ForbiddenException("You can only view your own block list");
        }
        return ResponseEntity.ok(relationService.getBlocking(userId, cursor, limit));
    }

    @PostMapping("/follow")
    public ResponseEntity<Void> follow(
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt) {
        relationService.follow(UUID.fromString(jwt.getSubject()), userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/follow")
    public ResponseEntity<Void> unfollow(
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt) {
        relationService.unfollow(UUID.fromString(jwt.getSubject()), userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/block")
    public ResponseEntity<Void> block(
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt) {
        relationService.block(UUID.fromString(jwt.getSubject()), userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/block")
    public ResponseEntity<Void> unblock(
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt) {
        relationService.unblock(UUID.fromString(jwt.getSubject()), userId);
        return ResponseEntity.noContent().build();
    }
}