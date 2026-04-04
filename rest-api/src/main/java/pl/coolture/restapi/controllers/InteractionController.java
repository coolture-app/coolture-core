package pl.coolture.restapi.controllers;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.coolture.restapi.dtos.Post.InteractionDTO;
import pl.coolture.restapi.dtos.Post.PostCommentDTO;
import pl.coolture.restapi.services.InteractionService;
import pl.coolture.restapi.utils.InteractionChangeInfo;


@RestController
@RequestMapping("/interactions")
@RequiredArgsConstructor
public class InteractionController {
  private final InteractionService interactionService;

  @PatchMapping("/like")
  public ResponseEntity<Void> likePost(@RequestBody @Valid InteractionDTO interactionDTO) {
    InteractionChangeInfo info = new InteractionChangeInfo(true, false);
    interactionService.changeInteractionState(interactionDTO, info);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/participate")
  public ResponseEntity<Void> participatePost(@RequestBody @Valid InteractionDTO interactionDTO) {
    InteractionChangeInfo info = new InteractionChangeInfo(false, true);
    interactionService.changeInteractionState(interactionDTO, info);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/comment")
  public ResponseEntity<PostCommentDTO> commentPost(
      @RequestBody @Valid PostCommentDTO postCommentDTO) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(interactionService.commentPost(postCommentDTO));
  }

  @GetMapping
  public ResponseEntity<InteractionChangeInfo> getInteractionState(
      @RequestParam @Valid InteractionDTO dto) {
    return ResponseEntity.ok(
        interactionService.getInteractionState(dto.getUserUuid(), dto.getPostUuid()));
  }
}
