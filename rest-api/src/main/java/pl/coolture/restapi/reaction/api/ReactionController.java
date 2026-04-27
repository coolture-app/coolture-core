package pl.coolture.restapi.reaction.api;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pl.coolture.restapi.common.config.security.SecurityUtils;
import pl.coolture.restapi.reaction.api.dto.ReactionRequest;
import pl.coolture.restapi.reaction.application.ReactionService;

@RestController
@RequiredArgsConstructor
public class ReactionController {

    private final ReactionService reactionService;

    @PutMapping("/posts/{postId}/reaction")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void upsert(
            @PathVariable UUID postId,
            @Valid @RequestBody ReactionRequest request) {
        reactionService.upsert(postId, SecurityUtils.getCurrentUserId(), request);
    }

    @DeleteMapping("/posts/{postId}/reaction")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID postId) {
        reactionService.delete(postId, SecurityUtils.getCurrentUserId());
    }
}
