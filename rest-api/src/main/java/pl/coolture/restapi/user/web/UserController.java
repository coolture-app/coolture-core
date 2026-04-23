package pl.coolture.restapi.user.web;


import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.coolture.restapi.common.config.security.SecurityUtils;
import pl.coolture.restapi.user.application.UserService;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;

  @GetMapping("/me")
  public String getCurrentUser() {
    return SecurityUtils.getCurrentUserId();
  }
}
