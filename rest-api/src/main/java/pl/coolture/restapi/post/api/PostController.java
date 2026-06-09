package pl.coolture.restapi.post.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pl.coolture.restapi.common.config.security.SecurityUtils;
import pl.coolture.restapi.common.pagination.CursorPage;
import pl.coolture.restapi.post.api.dto.*;
import pl.coolture.restapi.post.application.PostFeedFilters;
import pl.coolture.restapi.post.application.PostService;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping("/recommendations")
    public CursorPage<PostCardDto> getRecommendations(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return postService.getRecommendations(SecurityUtils.getCurrentUserId(), cursor, limit);
    }

    // ModelAttribute annotation treats every field as optional
    // ParameterObject annotation for Swagger UI to treat it as query params
    @GetMapping
    public CursorPage<PostCardDto> getFeed(@ParameterObject @ModelAttribute @Valid PostFeedRequest req) {
        UUID callerId = SecurityUtils.getCurrentUserIdOrNull();
        return postService.getFeed(callerId, req.toFilters(), req.getCursor(), req.getLimit());
    }

    @GetMapping("/{postId}")
    public PostDetailDto getById(@PathVariable UUID postId) {
        UUID callerId = SecurityUtils.getCurrentUserIdOrNull();
        return postService.getById(postId, callerId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostDetailDto create(@Valid @RequestBody PostCreateRequest request) {
        return postService.create(SecurityUtils.getCurrentUserId(), request);
    }

    @PatchMapping("/{postId}")
    public PostDetailDto update(
            @PathVariable UUID postId,
            @Valid @RequestBody PostUpdateRequest request) {
        return postService.update(postId, SecurityUtils.getCurrentUserId(), request);
    }

    @DeleteMapping("/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID postId) {
        postService.softDelete(postId, SecurityUtils.getCurrentUserId());
    }

    @GetMapping("/map")
    public List<PostMarkDto> getMarks(@ParameterObject @ModelAttribute @Valid PostFeedRequest req,
                                      @ParameterObject @ModelAttribute @Valid MapBoundsDto mapBounds) {
        UUID callerId = SecurityUtils.getCurrentUserIdOrNull();
        return postService.getPostMarks(callerId, req.toFilters(), mapBounds);
    }
}