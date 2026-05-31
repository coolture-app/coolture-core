package pl.coolture.restapi.post.application;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.coolture.restapi.comment.application.CommentService;
import pl.coolture.restapi.common.exceptions.BadRequestException;
import pl.coolture.restapi.common.exceptions.ForbiddenException;
import pl.coolture.restapi.common.exceptions.ResourceNotFoundException;
import pl.coolture.restapi.common.exceptions.UnauthenticatedUserException;
import pl.coolture.restapi.common.pagination.CursorCodec;
import pl.coolture.restapi.common.pagination.CursorPage;
import pl.coolture.restapi.common.pagination.CursorPayload;
import pl.coolture.restapi.media.api.dto.MediaResourceDto;
import pl.coolture.restapi.media.domain.Media;
import pl.coolture.restapi.media.domain.MediaRepository;
import pl.coolture.restapi.participation.application.ParticipationService;
import pl.coolture.restapi.participation.domain.ParticipationType;
import pl.coolture.restapi.post.api.PostMapper;
import pl.coolture.restapi.post.api.dto.*;
import pl.coolture.restapi.post.domain.Post;
import pl.coolture.restapi.post.domain.PostMedia;
import pl.coolture.restapi.post.domain.PostRepository;
import pl.coolture.restapi.reaction.application.ReactionService;
import pl.coolture.restapi.reaction.domain.ReactionType;
import pl.coolture.restapi.user.application.UserAvatarService;
import pl.coolture.restapi.user.domain.User;
import pl.coolture.restapi.user.domain.UserRepository;
import pl.coolture.restapi.common.config.storage.PresignService;
import pl.coolture.restapi.post.domain.PostType;
import pl.coolture.restapi.post.domain.PostStatus;
import pl.coolture.restapi.post.domain.PostVisibility;
import org.springframework.context.ApplicationEventPublisher;
import pl.coolture.restapi.embedding.domain.PostEmbeddingMessage;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {



  private static final Set<String> VALID_PARTICIPATION_TYPES = Set.of("INTERESTED", "TAKES_PART");
  private static final Set<String> VALID_REACTION_TYPES      = Set.of("LIKE", "DISLIKE");

  private final PostRepository postRepository;
  private final UserRepository userRepository;
  private final MediaRepository mediaRepository;
  private final PostMapper postMapper;
  private final CursorCodec cursorCodec;
  private final CommentService commentService;
  private final ReactionService reactionService;
  private final ParticipationService participationService;
  private final UserAvatarService userAvatarService;
  private final ApplicationEventPublisher eventPublisher;

  public CursorPage<PostCardDto> getRecommendations(UUID callerId, String cursor, int limit) {
    var payload = cursorCodec.decode(cursor);

    List<Post> rows = postRepository.findRecommendations(
        callerId,
        payload.map(CursorPayload::createdAt).orElse(null),
        payload.map(CursorPayload::id).orElse(null),
        limit + 1);

    Map<UUID, MediaResourceDto> avatars = userAvatarService.resolveThumbnails(
        rows.stream().map(p -> p.getAuthor().getId()).toList());

    List<PostCardDto> dtos = rows.stream()
        .map(p -> postMapper.toCard(p, avatars.get(p.getAuthor().getId())))
        .toList();

    enrich(dtos, callerId);
    return CursorPage.of(dtos, limit, PostCardDto::getId, PostCardDto::getCreatedAt, cursorCodec);
  }
  private final PresignService presignService;

  /**
   * Paginated feed with optional filters and cursor pagination.
   *
   * callerId may be null for unauthenticated callers myReaction / myParticipation will remain
   * null in that case.
   *
   * Passing participationTypes without authentication throws UnauthenticatedUserException
   *
   * TODO visibility enforcement: currently the `visibility` param is a plain filter. Proper
   *  rules (PRIVATE = author only, FRIENDS = followers only)
   */
  public CursorPage<PostCardDto> getFeed(
      UUID callerId, PostFeedFilters f, String cursor, int limit) {

    validateParticipationFilter(f.participationTypes(), callerId);
    validateReactionFilter(f.reactionType(), callerId);

    var payload = cursorCodec.decode(cursor);

    Double radiusMeters = (f.radiusKm() == null) ? null : f.radiusKm() * 1000.0;
    String visibilityFilter = getVisibilityFilter(f.visibility(), callerId);

    List<Post> rows =
        postRepository.findFeed(
            blankToNull(f.q()),
            toNullableArray(f.tags()),
            f.authorId(),
            f.status() != null ? f.status().name() : null,
            visibilityFilter,
            f.type() != null ? f.type().name() : null,
            f.startsFrom(),
            f.startsTo(),
            f.latitude(),
            f.longitude(),
            radiusMeters,
            toNullableArray(f.participationTypes()),
            f.reactionType(),
            callerId,
            payload.map(CursorPayload::createdAt).orElse(null),
            payload.map(CursorPayload::id).orElse(null),
            blankToNull(f.sortBy()),
            limit + 1);

    List<UUID> authorIds = rows
            .stream()
            .map(p -> p.getAuthor().getId())
            .toList();

    Map<UUID, MediaResourceDto> avatars = userAvatarService.resolveThumbnails(authorIds);

    List<PostCardDto> dtos = rows.stream()
            .map(p -> postMapper.toCard(p, avatars.get(p.getAuthor().getId())))
            .toList();

    enrich(dtos, callerId);
    return CursorPage.of(dtos, limit, PostCardDto::getId, PostCardDto::getCreatedAt, cursorCodec);
  }

  /** callerId may be null - myReaction / myParticipation will remain null in that case. */
  public PostDetailDto getById(UUID postId, UUID callerId) {
    Post post = findActiveOrThrow(postId);

    PostDetailDto dto = postMapper.toDetail(
            post,
            userAvatarService.resolveThumbnail(post.getAuthor().getId()));

    if (callerId != null) {
      Map<UUID, ReactionType> reactions =
          reactionService.findReactionTypesForPosts(callerId, List.of(postId));

      Map<UUID, ParticipationType> participations =
          participationService.findParticipationTypesForPosts(callerId, List.of(postId));

      dto.setMyReaction(reactions.get(postId));
      dto.setMyParticipation(participations.get(postId));
    }
    return dto;
  }

  @Transactional
  public PostDetailDto create(UUID callerId, PostCreateRequest req) {
    validateTypeLocationInvariant(req.type(), req.location());
    validateDateRange(req.startsAt(), req.endsAt());

    User author = userRepository.getReferenceById(callerId);
    Post post =
        Post.builder()
            .author(author)
            .location(postMapper.toLocationEntity(req.location()))
            .title(req.title())
            .description(req.description())
            .eventUrl(req.eventUrl())
            .startsAt(req.startsAt())
            .endsAt(req.endsAt())
            .tags(toNullableArray(req.tags()))
            .type(req.type())
            .status(PostStatus.ACTIVE)
            .visibility(req.visibility() != null ? req.visibility() : PostVisibility.PUBLIC)
            .createdAt(Instant.now())
            .media(new ArrayList<>())
            .build();

    attachMedia(post, callerId, req.mediaIds(), req.coverMediaId());

    post = postRepository.save(post);
    eventPublisher.publishEvent(
        new PostEmbeddingMessage(post.getId(), post.getTitle(), post.getDescription(),
            post.getTags() != null ? List.of(post.getTags()) : List.of(), post.getCreatedAt()));
    return postMapper.toDetail(
            post,
            userAvatarService.resolveThumbnail(post.getAuthor().getId()));
  }

  @Transactional
  public PostDetailDto update(UUID postId, UUID callerId, PostUpdateRequest req) {
    Post post = findActiveOrThrow(postId);
    requireAuthor(post, callerId);

    // Resolve type + location (online events no location)
    PostType newType = req.type() != null ? req.type() : post.getType();
    boolean locationTouched = req.location() != null;
    EventLocationDto newLocDto =
        locationTouched ? req.location() : postMapper.toLocationDto(post.getLocation());
    validateTypeLocationInvariant(newType, newLocDto);

    Instant newStartsAt = req.startsAt() != null ? req.startsAt() : post.getStartsAt();
    Instant newEndsAt = req.endsAt() != null ? req.endsAt() : post.getEndsAt();
    validateDateRange(newStartsAt, newEndsAt);

    if (req.title() != null) post.setTitle(req.title());
    if (req.description() != null) post.setDescription(req.description());
    if (req.eventUrl() != null) post.setEventUrl(req.eventUrl());
    if (req.startsAt() != null) post.setStartsAt(req.startsAt());
    if (req.endsAt() != null) post.setEndsAt(req.endsAt());
    if (req.tags() != null) post.setTags(toNullableArray(req.tags()));
    if (req.type() != null) post.setType(req.type());
    if (req.visibility() != null) post.setVisibility(req.visibility());

    if (locationTouched) {
      if (PostType.ONLINE == newType) {
        post.setLocation(null);
      } else if (post.getLocation() == null) {
        post.setLocation(postMapper.toLocationEntity(req.location()));
      } else {
        postMapper.updateLocation(post.getLocation(), req.location());
      }
    } else if (PostType.ONLINE == newType) {
      // Type switched to ONLINE: drop any existing location.
      post.setLocation(null);
    }

    if (req.mediaIds() != null) {
      post.getMedia().clear();
      attachMedia(post, callerId, req.mediaIds(), req.coverMediaId());
    } else if (req.coverMediaId() != null) {
      // Only the cover flag is being moved - keep media rows as-is.
      updateCoverFlag(post, req.coverMediaId());
    }

    post.setStatus(PostStatus.EDITED);
    post.setLastModifiedAt(Instant.now());
    return postMapper.toDetail(
            post,
            userAvatarService.resolveThumbnail(post.getAuthor().getId()));
  }

  @Transactional
  public void softDelete(UUID postId, UUID callerId) {
    Post post = findActiveOrThrow(postId);
    requireAuthor(post, callerId);

    commentService.deleteAllForPost(postId);

    post.setStatus(PostStatus.DELETED);
    post.setDeletedAt(Instant.now());
  }

  public List<PostMarkDto> getPostMarks(
          UUID callerId, PostFeedFilters f, MapBoundsDto mapBounds){

    String visibilityFilter = getVisibilityFilter(f.visibility(), callerId);

    Double minLng = mapBounds.leftUpper().longitude();
    Double maxLat = mapBounds.leftUpper().latitude();

    Double maxLng = mapBounds.rightBottom().longitude();
    Double minLat = mapBounds.rightBottom().latitude();

    List<Post> rows = postRepository.findAllMapMarks(
            f.q(),
            toNullableArray(f.tags()),
            f.authorId(),
            f.status() != null ? f.status().name() : null,
            visibilityFilter,
            f.type() != null ? f.type().name() : null,
            f.startsFrom(),
            f.startsTo(),
            minLng,
            minLat,
            maxLng,
            maxLat,
            toNullableArray(f.participationTypes()),
            f.reactionType(),
            callerId
    );

    // Batch-fetch cover media object keys and presign URLs (avoids N+1 selects)
    Map<UUID, String> coverUrls = resolveCoverMediaUrls(rows);

    return rows.stream()
            .map(p -> postMapper.toPostMarkDto(p, coverUrls.get(p.getId())))
            .toList();
  }


  /**
   * Batch fetch of caller's reactions and participations for a page of posts, then sets them via
   * the mutable fields on each DTO.
   */
  private void enrich(List<PostCardDto> dtos, UUID callerId) {
    if (callerId == null || dtos.isEmpty()) return;

    List<UUID> postIds = dtos.stream().map(PostCardDto::getId).toList();
    Map<UUID, ReactionType> reactions =
        reactionService.findReactionTypesForPosts(callerId, postIds);
    Map<UUID, ParticipationType> participations =
        participationService.findParticipationTypesForPosts(callerId, postIds);

    dtos.forEach(
        dto -> {
          dto.setMyReaction(reactions.get(dto.getId()));
          dto.setMyParticipation(participations.get(dto.getId()));
        });
  }

  private Post findActiveOrThrow(UUID postId) {
    Post post =
        postRepository
            .findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Post", postId));
    if (PostStatus.DELETED == post.getStatus() || post.getDeletedAt() != null) {
      throw new ResourceNotFoundException("Post", postId);
    }
    return post;
  }

  private void requireAuthor(Post post, UUID callerId) {
    if (!post.getAuthor().getId().equals(callerId)) {
      throw new ForbiddenException("Only the author can modify this post");
    }
  }

  private void validateTypeLocationInvariant(PostType type, EventLocationDto location) {
    if (PostType.OFFLINE == type && location == null) {
      throw new ForbiddenException("OFFLINE posts require a location");
    }
    if (PostType.ONLINE == type && location != null) {
      throw new ForbiddenException("ONLINE posts cannot have a location");
    }
  }

  private void validateDateRange(Instant startsAt, Instant endsAt) {
    if (endsAt != null && !endsAt.isAfter(startsAt)) {
      throw new ForbiddenException("endsAt must be after startsAt");
    }
  }

  /**
   * Validates that the participation filter is used only by authenticated callers and that every
   * supplied type is a known value.
   */
  private void validateParticipationFilter(List<String> participationTypes, UUID callerId) {
    if (participationTypes == null || participationTypes.isEmpty()) return;

    if (callerId == null) {
      throw new UnauthenticatedUserException(
          "Authentication is required to filter by participation type");
    }

    List<String> unknown =
        participationTypes.stream().filter(t -> !VALID_PARTICIPATION_TYPES.contains(t)).toList();
    if (!unknown.isEmpty()) {
      throw new BadRequestException(
          "Unknown participation type(s): "
              + unknown
              + ". Allowed values: "
              + VALID_PARTICIPATION_TYPES);
    }
  }

  private void validateReactionFilter(String reactionType, UUID callerId) {
    if (reactionType == null) return;

    if (callerId == null) {
      throw new UnauthenticatedUserException(
              "Authentication is required to filter by reaction type");
    }

    if (!VALID_REACTION_TYPES.contains(reactionType)) {
      throw new BadRequestException(
              "Unknown reaction type: " + reactionType
                + ". Allowed values: " + VALID_REACTION_TYPES);
    }
  }

  /**
   * Validates every media ID, builds PostMedia rows in the given order and flips the cover flag on
   * the matching row.
   */
  private void attachMedia(Post post, UUID callerId, List<UUID> mediaIds, UUID coverMediaId) {
    if (mediaIds == null || mediaIds.isEmpty()) return;

    if (new HashSet<>(mediaIds).size() != mediaIds.size()) {
      throw new ForbiddenException("mediaIds must be unique");
    }
    if (coverMediaId != null && !mediaIds.contains(coverMediaId)) {
      throw new ForbiddenException("coverMediaId must be present in mediaIds");
    }

    Map<UUID, Media> byId =
        mediaRepository.findAllById(mediaIds).stream()
            .collect(Collectors.toMap(Media::getId, Function.identity()));

    for (int i = 0; i < mediaIds.size(); i++) {
      UUID mid = mediaIds.get(i);
      Media m = byId.get(mid);
      if (m == null || "DELETED".equals(m.getStatus())) {
        throw new ResourceNotFoundException("Media", mid);
      }
      if (!m.getOwnerId().equals(callerId)) {
        throw new ForbiddenException("You do not own media " + mid);
      }
      post.getMedia()
          .add(
              PostMedia.builder()
                  .post(post)
                  .media(m)
                  .position(i)
                  .isCover(mid.equals(coverMediaId))
                  .build());
    }
  }

  private void updateCoverFlag(Post post, UUID coverMediaId) {
    boolean found = false;
    for (PostMedia pm : post.getMedia()) {
      boolean match = pm.getMedia().getId().equals(coverMediaId);
      pm.setCover(match);
      found = found || match;
    }
    if (!found) {
      throw new ForbiddenException("coverMediaId is not attached to this post");
    }
  }

  private static String[] toNullableArray(List<String> list) {
    return (list == null || list.isEmpty()) ? null : list.toArray(String[]::new);
  }

  private static String blankToNull(String s) {
    return (s == null || s.isBlank()) ? null : s;
  }

  private String getVisibilityFilter(PostVisibility explicitFilter, UUID callerId) {
    if (explicitFilter != null) {
      return explicitFilter.name();
    }
    if (callerId == null) {
      return PostVisibility.PUBLIC.name();
    }
    return null;
  }

  /**
   * Batch-fetches cover media S3 object keys for the given posts and presigns GET URLs.
   * Returns a map from postId to presigned cover media URL.
   */
  private Map<UUID, String> resolveCoverMediaUrls(List<Post> posts) {
    if (posts.isEmpty()) return Map.of();

    UUID[] postIds = posts.stream().map(Post::getId).toArray(UUID[]::new);
    List<Object[]> rows = postRepository.findCoverMediaKeys(postIds);

    return rows.stream()
            .collect(Collectors.toMap(
                    row -> (UUID) row[0],
                    row -> presignService.presignGet((String) row[1]).url().toString()));
  }
}
