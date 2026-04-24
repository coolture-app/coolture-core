package pl.coolture.restapi.post.api;

import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pl.coolture.restapi.common.config.security.SecurityUtils;
import pl.coolture.restapi.common.pagination.CursorPage;
import pl.coolture.restapi.post.api.dto.PostCardDto;
import pl.coolture.restapi.post.api.dto.PostCreateRequest;
import pl.coolture.restapi.post.api.dto.PostDetailDto;
import pl.coolture.restapi.post.api.dto.PostUpdateRequest;
import pl.coolture.restapi.post.application.PostFeedFilters;
import pl.coolture.restapi.post.application.PostService;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping
    public CursorPage<PostCardDto> getFeed(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) UUID authorId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String visibility,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Instant startsFrom,
            @RequestParam(required = false) Instant startsTo,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {

        var filters = new PostFeedFilters(
                q, categoryId, tags, authorId, status, visibility, type,
                startsFrom, startsTo, latitude, longitude, radiusKm);
        return postService.getFeed(filters, cursor, limit);
    }

    @GetMapping("/{postId}")
    public PostDetailDto getById(@PathVariable UUID postId) {
        return postService.getById(postId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostDetailDto create(@Valid @RequestBody PostCreateRequest request) {
        return postService.create(UUID.fromString(SecurityUtils.getCurrentUserId()), request);
    }

    @PatchMapping("/{postId}")
    public PostDetailDto update(
            @PathVariable UUID postId,
            @Valid @RequestBody PostUpdateRequest request) {
        return postService.update(postId, UUID.fromString(SecurityUtils.getCurrentUserId()), request);
    }

    @DeleteMapping("/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID postId) {
        postService.softDelete(postId, UUID.fromString(SecurityUtils.getCurrentUserId()));
    }
}