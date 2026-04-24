package pl.coolture.restapi.user.application;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.coolture.restapi.common.exceptions.ForbiddenException;
import pl.coolture.restapi.common.exceptions.ResourceNotFoundException;
import pl.coolture.restapi.common.pagination.CursorCodec;
import pl.coolture.restapi.common.pagination.CursorPage;
import pl.coolture.restapi.common.pagination.CursorPayload;
import pl.coolture.restapi.relation.application.RelationService;
import pl.coolture.restapi.user.api.UserMapper;
import pl.coolture.restapi.user.api.dto.UserProfileDto;
import pl.coolture.restapi.user.api.dto.UserProfileUpdateRequest;
import pl.coolture.restapi.user.api.dto.UserSummaryDto;
import pl.coolture.restapi.user.domain.User;
import pl.coolture.restapi.user.domain.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

  private final UserRepository userRepository;
  private final UserMapper     userMapper;
  private final CursorCodec    cursorCodec;

  /**
   * @Lazy breaks the UserService <-> RelationService circular dependency:
   */
  @Lazy
  private final RelationService relationService;

  public CursorPage<UserSummaryDto> search(String q, String cursor, int limit) {
    var payload = cursorCodec.decode(cursor);

    var rows = userRepository.search(
            (q != null && !q.isBlank()) ? q : null,
            payload.map(CursorPayload::createdAt).orElse(null),
            payload.map(CursorPayload::id).orElse(null),
            limit + 1);  // +1 to detect next page

    var dtos = rows.stream().map(userMapper::toSummaryDto).toList();

    return CursorPage.of(dtos, limit, UserSummaryDto::id, UserSummaryDto::createdAt, cursorCodec);
  }

  public UserProfileDto getById(UUID id) {
    return userMapper.toProfileDto(findOrThrow(id));
  }

  /** enriches profile with caller's relation context. */
  public UserProfileDto getByIdForCaller(UUID targetId, UUID callerId) {
    var profile = userMapper.toProfileDto(findOrThrow(targetId));
    return targetId.equals(callerId) ? profile : withRelations(profile, callerId, targetId);
  }

  /** enriches profile with caller's relation context. */
  public UserProfileDto getByUsernameForCaller(String username, UUID callerId) {
    var user    = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", username));
    var profile = userMapper.toProfileDto(user);
    return user.getId().equals(callerId) ? profile : withRelations(profile, callerId, user.getId());
  }

  @Transactional
  public UserProfileDto update(UUID targetId, UUID callerId, UserProfileUpdateRequest request) {
    if (!targetId.equals(callerId)) {
      throw new ForbiddenException("You can only update your own profile");
    }

    User user = findOrThrow(targetId);
    userMapper.updateEntity(request, user);
    return userMapper.toProfileDto(user);
  }

  /**
   * builds the profile record with live isFollowing / isBlocked values.
   * TODO: populate avatar here from active profile_image.
   */
  private UserProfileDto withRelations(UserProfileDto p, UUID callerId, UUID targetId) {
    return new UserProfileDto(
            p.id(), p.username(), p.firstName(), p.lastName(),
            p.avatar(),
            p.followersCount(), p.followingCount(),
            p.bio(), p.createdAt(),
            relationService.isFollowing(callerId, targetId),
            relationService.isBlocked(callerId, targetId));
  }

  private User findOrThrow(UUID id) {
    return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
  }
}