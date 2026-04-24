package pl.coolture.restapi.user.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.coolture.restapi.common.pagination.CursorPage;
import pl.coolture.restapi.user.api.dto.UserProfileDto;
import pl.coolture.restapi.user.api.dto.UserProfileUpdateRequest;
import pl.coolture.restapi.user.api.dto.UserSummaryDto;
import pl.coolture.restapi.user.application.UserService;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Validated
public class UserController {

  private final UserService userService;

  @GetMapping("/by-username/{username}")
  public ResponseEntity<UserProfileDto> getByUsername(
          @PathVariable String username,
          @AuthenticationPrincipal Jwt jwt) {
    UUID callerId = UUID.fromString(jwt.getSubject());
    return ResponseEntity.ok(userService.getByUsernameForCaller(username, callerId));
  }

  /** GET /users?q=&cursor=&limit= */
  @GetMapping
  public ResponseEntity<CursorPage<UserSummaryDto>> search(
          @RequestParam(required = false) String q,
          @RequestParam(required = false) String cursor,
          @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {

    return ResponseEntity.ok(userService.search(q, cursor, limit));
  }

  @GetMapping("/{userId}")
  public ResponseEntity<UserProfileDto> getById(
          @PathVariable UUID userId,
          @AuthenticationPrincipal Jwt jwt) {
    UUID callerId = UUID.fromString(jwt.getSubject());
    return ResponseEntity.ok(userService.getByIdForCaller(userId, callerId));
  }


  @PatchMapping("/{userId}")
  public ResponseEntity<UserProfileDto> update(
          @PathVariable UUID userId,
          @Valid @RequestBody UserProfileUpdateRequest request,
          @AuthenticationPrincipal Jwt jwt) {

    UUID callerId = UUID.fromString(jwt.getSubject());
    return ResponseEntity.ok(userService.update(userId, callerId, request));
  }
}