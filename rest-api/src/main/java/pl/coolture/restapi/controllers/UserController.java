package pl.coolture.restapi.controllers;


import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.coolture.restapi.common.config.security.SecurityUtils;
import pl.coolture.restapi.dtos.User.CreateUpdateUserDTO;
import pl.coolture.restapi.dtos.User.GetCurrentUserDTO;
import pl.coolture.restapi.services.UserService;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;

  @GetMapping("/me")
  public String getCurrentUser() {
    return SecurityUtils.getCurrentUserId();
  }

  @GetMapping("/{id}")
  public ResponseEntity<CreateUpdateUserDTO> getUserById(@PathVariable UUID id) {
    return ResponseEntity.ok(userService.getUserById(id));
  }

  @GetMapping("/username/{username}")
  public ResponseEntity<CreateUpdateUserDTO> getUserByUsername(@PathVariable String username) {
    return ResponseEntity.ok(userService.getUserByUsername(username));
  }

  @PostMapping
  public ResponseEntity<CreateUpdateUserDTO> createUser(
      @RequestBody @Valid CreateUpdateUserDTO createUpdateUserDTO) {
    CreateUpdateUserDTO createdUser = userService.createUser(createUpdateUserDTO);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.subject")
  public ResponseEntity<CreateUpdateUserDTO> updateUser(
      @PathVariable UUID id, @RequestBody @Valid CreateUpdateUserDTO createUpdateUserDTO) {
    CreateUpdateUserDTO updatedUser = userService.updateUser(id, createUpdateUserDTO);
    return ResponseEntity.ok(updatedUser);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
    userService.deleteUser(id);
    return ResponseEntity.noContent().build();
  }
}
