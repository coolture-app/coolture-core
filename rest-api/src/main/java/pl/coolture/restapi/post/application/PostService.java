package pl.coolture.restapi.post.application;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
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
import pl.coolture.restapi.dictionary.domain.EventCategory;
import pl.coolture.restapi.dictionary.domain.EventCategoryRepository;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

  private static final String TYPE_ONLINE = "ONLINE";
  private static final String TYPE_OFFLINE = "OFFLINE";
  private static final String STATUS_ACTIVE = "ACTIVE";
  private static final String STATUS_EDITED = "EDITED";
  private static final String STATUS_DELETED = "DELETED";
  private static final String VISIBILITY_PUBLIC = "PUBLIC";

  private static final Set<String> VALID_PARTICIPATION_TYPES = Set.of("interested", "takes_part");
  private static final Set<String> VALID_REACTION_TYPES      = Set.of("like", "dislike");

  private final PostRepository postRepository;
  private final UserRepository userRepository;
  private final EventCategoryRepository categoryRepository;
  private final MediaRepository mediaRepository;
  private final PostMapper postMapper;
  private final CursorCodec cursorCodec;
  private final CommentService commentService;
  private final ReactionService reactionService;
  private final ParticipationService participationService;
  private final UserAvatarService userAvatarService;

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

    List<Post> rows =
        postRepository.findFeed(
            blankToNull(f.q()),
            f.categoryId(),
            toNullableArray(f.tags()),
            f.authorId(),
            f.status(),
            f.visibility(),
            f.type(),
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
    EventCategory cat =
        categoryRepository
            .findById(req.categoryId())
            .orElseThrow(() -> new ResourceNotFoundException("EventCategory", req.categoryId()));

    Post post =
        Post.builder()
            .author(author)
            .category(cat)
            .location(postMapper.toLocationEntity(req.location()))
            .title(req.title())
            .description(req.description())
            .eventUrl(req.eventUrl())
            .startsAt(req.startsAt())
            .endsAt(req.endsAt())
            .tags(toNullableArray(req.tags()))
            .type(req.type())
            .status(STATUS_ACTIVE)
            .visibility(req.visibility() != null ? req.visibility() : VISIBILITY_PUBLIC)
            .createdAt(Instant.now())
            .media(new ArrayList<>())
            .build();

    attachMedia(post, callerId, req.mediaIds(), req.coverMediaId());

    post = postRepository.save(post);
    return postMapper.toDetail(
            post,
            userAvatarService.resolveThumbnail(post.getAuthor().getId()));
  }

  @Transactional
  public PostDetailDto update(UUID postId, UUID callerId, PostUpdateRequest req) {
    Post post = findActiveOrThrow(postId);
    requireAuthor(post, callerId);

    // Resolve type + location (online events no location)
    String newType = req.type() != null ? req.type() : post.getType();
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

    if (req.categoryId() != null) {
      post.setCategory(
          categoryRepository
              .findById(req.categoryId())
              .orElseThrow(() -> new ResourceNotFoundException("EventCategory", req.categoryId())));
    }

    if (locationTouched) {
      if (TYPE_ONLINE.equals(newType)) {
        post.setLocation(null);
      } else if (post.getLocation() == null) {
        post.setLocation(postMapper.toLocationEntity(req.location()));
      } else {
        postMapper.updateLocation(post.getLocation(), req.location());
      }
    } else if (TYPE_ONLINE.equals(newType)) {
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

    post.setStatus(STATUS_EDITED);
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

    post.setStatus(STATUS_DELETED);
    post.setDeletedAt(Instant.now());
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
    if (STATUS_DELETED.equals(post.getStatus()) || post.getDeletedAt() != null) {
      throw new ResourceNotFoundException("Post", postId);
    }
    return post;
  }

  private void requireAuthor(Post post, UUID callerId) {
    if (!post.getAuthor().getId().equals(callerId)) {
      throw new ForbiddenException("Only the author can modify this post");
    }
  }

  private void validateTypeLocationInvariant(String type, EventLocationDto location) {
    if (TYPE_OFFLINE.equals(type) && location == null) {
      throw new ForbiddenException("OFFLINE posts require a location");
    }
    if (TYPE_ONLINE.equals(type) && location != null) {
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
}
