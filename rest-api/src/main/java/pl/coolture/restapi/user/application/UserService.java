package pl.coolture.restapi.user.application;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.coolture.restapi.common.exceptions.ForbiddenException;
import pl.coolture.restapi.common.exceptions.ResourceNotFoundException;
import pl.coolture.restapi.common.pagination.CursorCodec;
import pl.coolture.restapi.common.pagination.CursorPage;
import pl.coolture.restapi.common.pagination.CursorPayload;
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

  public UserProfileDto getByUsername(String username) {
    return userRepository.findByUsername(username)
            .map(userMapper::toProfileDto)
            .orElseThrow(() -> new ResourceNotFoundException("User", username));
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

  private User findOrThrow(UUID id) {
    return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
  }
}