package pl.coolture.restapi.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.coolture.restapi.services.ImageService;

@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
public class ImageController {
  private final ImageService imageService;

  @GetMapping("/posts/{id}")
  public ResponseEntity<Resource> getImage(@PathVariable String id) {
    return ResponseEntity.ok(imageService.getImage(id, "posts"));
  }

  @GetMapping("/avatars/{id}")
  public ResponseEntity<Resource> getAvatar(@PathVariable String id) {
    return ResponseEntity.ok(imageService.getImage(id, "avatars "));
  }
}
