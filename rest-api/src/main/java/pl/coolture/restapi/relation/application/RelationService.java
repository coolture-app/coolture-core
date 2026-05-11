package pl.coolture.restapi.relation.application;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.coolture.restapi.common.exceptions.ConflictException;
import pl.coolture.restapi.common.exceptions.ResourceNotFoundException;
import pl.coolture.restapi.common.pagination.CursorCodec;
import pl.coolture.restapi.common.pagination.CursorPage;
import pl.coolture.restapi.common.pagination.CursorPayload;
import pl.coolture.restapi.relation.domain.UserRelation;
import pl.coolture.restapi.relation.domain.UserRelationId;
import pl.coolture.restapi.relation.domain.UserRelationRepository;
import pl.coolture.restapi.relation.domain.UserRelationType;
import pl.coolture.restapi.user.api.UserMapper;
import pl.coolture.restapi.user.api.dto.UserSummaryDto;
import pl.coolture.restapi.user.application.UserAvatarService;
import pl.coolture.restapi.user.domain.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RelationService {

    private static final UserRelationType FOLLOW = UserRelationType.FOLLOW;
    private static final UserRelationType BLOCK  = UserRelationType.BLOCK;

    private final UserRelationRepository relationRepository;
    private final UserRepository         userRepository;
    private final UserMapper             userMapper;
    private final CursorCodec            cursorCodec;
    private final UserAvatarService      userAvatarService;

    public CursorPage<UserSummaryDto> getFollowers(UUID userId, String cursor, int limit) {
        requireUserExists(userId);
        var payload = cursorCodec.decode(cursor);
        var rows = relationRepository.findFollowers(userId,
                payload.map(CursorPayload::createdAt).orElse(null),
                payload.map(CursorPayload::id).orElse(null),
                limit + 1);
        return toPage(rows, limit, UserRelation::getSourceUser);
    }

    public CursorPage<UserSummaryDto> getFollowing(UUID userId, String cursor, int limit) {
        requireUserExists(userId);
        var payload = cursorCodec.decode(cursor);
        var rows = relationRepository.findFollowing(userId,
                payload.map(CursorPayload::createdAt).orElse(null),
                payload.map(CursorPayload::id).orElse(null),
                limit + 1);
        return toPage(rows, limit, UserRelation::getTargetUser);
    }

    public CursorPage<UserSummaryDto> getBlocking(UUID userId, String cursor, int limit) {
        var payload = cursorCodec.decode(cursor);
        var rows = relationRepository.findBlocking(userId,
                payload.map(CursorPayload::createdAt).orElse(null),
                payload.map(CursorPayload::id).orElse(null),
                limit + 1);
        return toPage(rows, limit, UserRelation::getTargetUser);
    }

    @Transactional
    public void follow(UUID callerId, UUID targetId) {
        requireNotSelf(callerId, targetId, "follow");
        requireUserExists(targetId);

        var id = new UserRelationId(callerId, targetId);
        if (relationRepository.existsByIdAndType(id, FOLLOW)) {
            throw new ConflictException("Already following this user");
        }
        save(id, FOLLOW);
    }

    @Transactional
    public void unfollow(UUID callerId, UUID targetId) {
        requireUserExists(targetId);
        var id = new UserRelationId(callerId, targetId);
        int deleted = relationRepository.deleteByIdAndType(id, FOLLOW);
        if (deleted == 0) {
            throw new ResourceNotFoundException("Follow relation", targetId);
        }
    }

    @Transactional
    public void block(UUID callerId, UUID targetId) {
        requireNotSelf(callerId, targetId, "block");
        requireUserExists(targetId);

        var id = new UserRelationId(callerId, targetId);
        if (relationRepository.existsByIdAndType(id, BLOCK)) {
            throw new ConflictException("Already blocking this user");
        }

        // Replace any existing FOLLOW in the same direction with BLOCK
        relationRepository.deleteAny(callerId, targetId);
        // Also remove the reverse FOLLOW (target was following caller)
        relationRepository.deleteAny(targetId, callerId);

        save(id, BLOCK);
    }

    @Transactional
    public void unblock(UUID callerId, UUID targetId) {
        requireUserExists(targetId);
        var id = new UserRelationId(callerId, targetId);
        int deleted = relationRepository.deleteByIdAndType(id, BLOCK);
        if (deleted == 0) {
            throw new ResourceNotFoundException("Block relation", targetId);
        }
    }

    public boolean isFollowing(UUID sourceId, UUID targetId) {
        return relationRepository.existsByIdAndType(new UserRelationId(sourceId, targetId), FOLLOW);
    }

    public boolean isBlocked(UUID sourceId, UUID targetId) {
        return relationRepository.existsByIdAndType(new UserRelationId(sourceId, targetId), BLOCK);
    }

    private void save(UserRelationId id, UserRelationType type) {
        var source = userRepository.getReferenceById(id.getSourceUserId());
        var target = userRepository.getReferenceById(id.getTargetUserId());
        relationRepository.save(UserRelation.builder()
                .id(id)
                .type(type)
                .createdAt(Instant.now())
                .sourceUser(source)
                .targetUser(target)
                .build());
    }

    private CursorPage<UserSummaryDto> toPage(
            java.util.List<UserRelation> rows, int limit,
            java.util.function.Function<UserRelation, pl.coolture.restapi.user.domain.User> userExtractor) {

        var entityPage = CursorPage.of(rows, limit,
                r -> userExtractor.apply(r).getId(),
                UserRelation::getCreatedAt,
                cursorCodec);

        var userIds = entityPage
                .items()
                .stream()
                .map(r -> userExtractor.apply(r).getId())
                .toList();

        var avatars = userAvatarService.resolveThumbnails(userIds);

        var dtos = entityPage.items().stream()
                .map(r -> {
                    var user = userExtractor.apply(r);
                    return userMapper.toSummaryDto(user, avatars.get(user.getId()));
                })
                .toList();

        return new CursorPage<>(dtos, entityPage.page());
    }

    private void requireUserExists(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", userId);
        }
    }

    private void requireNotSelf(UUID callerId, UUID targetId, String action) {
        if (callerId.equals(targetId)) {
            throw new ConflictException("Cannot %s yourself".formatted(action));
        }
    }
}