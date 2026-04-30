package pl.coolture.restapi.user.application;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.coolture.restapi.media.api.dto.MediaResourceDto;
import pl.coolture.restapi.media.application.MediaService;
import pl.coolture.restapi.media.domain.Media;
import pl.coolture.restapi.media.domain.MediaRepository;
import pl.coolture.restapi.media.domain.ProfileImage;
import pl.coolture.restapi.media.domain.ProfileImageRepository;

/**
 * Resolves a user's active avatar (full or thumbnail) into a MediaResourceDto
 * Returns null when the user has no active profile image
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserAvatarService {

    private static final String STATUS_DELETED = "DELETED";

    private final ProfileImageRepository    profileImageRepository;
    private final MediaRepository           mediaRepository;
    private final MediaService              mediaService;

    /** Full-resolution avatar for profile detail views. */
    public MediaResourceDto resolveFull(UUID userId) {
        return resolveOne(userId, false);
    }

    /** Thumbnail for lists, feed cards, comment authors. */
    public MediaResourceDto resolveThumbnail(UUID userId) {
        return resolveOne(userId, true);
    }

    /**
     * Batch thumbnail lookup. Two queries total regardless of input size:
     * one for active profile_images, one for the referenced media rows.
     * Users without an avatar are absent from the returned map.
     */
    public Map<UUID, MediaResourceDto> resolveThumbnails(Collection<UUID> userIds) {
        if (userIds.isEmpty()) return Map.of();

        var images = profileImageRepository.findAllByUserIdInAndIsActiveTrue(userIds);
        if (images.isEmpty()) return Map.of();

        var mediaIds = images.
                stream()
                .map(ProfileImage::getThumbnailMediaId)
                .toList();

        var mediaById = mediaRepository
                .findAllById(mediaIds)
                .stream()
                .filter(m -> !STATUS_DELETED.equals(m.getStatus()))
                .collect(Collectors.toMap(Media::getId, Function.identity()));

        return images.stream()
                .filter(pi -> mediaById.containsKey(pi.getThumbnailMediaId()))
                .collect(Collectors.toMap(
                        ProfileImage::getUserId,
                        pi -> mediaService.toDto(mediaById.get(pi.getThumbnailMediaId()))));
    }

    private MediaResourceDto resolveOne(UUID userId, boolean thumbnail) {
        return profileImageRepository
                .findByUserIdAndIsActiveTrue(userId)
                .flatMap(pi -> mediaRepository.findById(
                        thumbnail ? pi.getThumbnailMediaId() : pi.getFullMediaId()))
                .filter(m -> !STATUS_DELETED.equals(m.getStatus()))
                .map(mediaService::toDto)
                .orElse(null);
    }
}