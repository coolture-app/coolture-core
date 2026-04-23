package pl.coolture.restapi.controllers;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pl.coolture.restapi.dtos.Post.CreateUpdatePostDTO;
import pl.coolture.restapi.dtos.Post.GetPostDTO;
import pl.coolture.restapi.services.PostService;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {
  private final PostService postService;

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<GetPostDTO> createPost(
          @RequestPart("postData") @Valid CreateUpdatePostDTO dto,
          @RequestPart(value = "images", required = false) List<MultipartFile> images) {
    return ResponseEntity.status(HttpStatus.CREATED).body(postService.createPost(dto, images));
  }

  @GetMapping
  public ResponseEntity<List<GetPostDTO>> getPosts(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
    return ResponseEntity.ok(postService.getPosts(page, size));
  }

  @GetMapping("/{id}")
  public ResponseEntity<GetPostDTO> getPost(@PathVariable UUID id) {
    return ResponseEntity.ok(postService.getPostById(id));
  }
}
