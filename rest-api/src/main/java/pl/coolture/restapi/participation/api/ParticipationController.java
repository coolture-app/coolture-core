package pl.coolture.restapi.participation.api;


import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pl.coolture.restapi.common.config.security.SecurityUtils;
import pl.coolture.restapi.participation.api.dto.ParticipationRequest;
import pl.coolture.restapi.participation.application.ParticipationService;

@RestController
@RequiredArgsConstructor
public class ParticipationController {

    private final ParticipationService participationService;

    @PutMapping("/posts/{postId}/participation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void upsert(
            @PathVariable UUID postId,
            @Valid @RequestBody ParticipationRequest request) {
        participationService.upsert(postId, SecurityUtils.getCurrentUserId(), request);
    }

    @DeleteMapping("/posts/{postId}/participation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID postId) {
        participationService.delete(postId, SecurityUtils.getCurrentUserId());
    }
}
