package pl.coolture.restapi.media.application;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.coolture.restapi.common.exceptions.ResourceNotFoundException;
import pl.coolture.restapi.media.api.dto.ProfileImageResponse;
import pl.coolture.restapi.media.api.dto.SetProfileImageRequest;
import pl.coolture.restapi.media.domain.Media;
import pl.coolture.restapi.media.domain.MediaRepository;
import pl.coolture.restapi.media.domain.ProfileImage;
import pl.coolture.restapi.media.domain.ProfileImageRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileImageService {

    private final ProfileImageRepository profileImageRepository;
    private final MediaRepository        mediaRepository;

    @Transactional
    public ProfileImageResponse set(UUID userId, SetProfileImageRequest request) {
        // Verify both media records exist and belong to the caller
        validateMedia(request.fullMediaId(), userId);
        validateMedia(request.thumbnailMediaId(), userId);

        // Deactivate whatever is currently active (if anything)
        profileImageRepository.deactivateAllForUser(userId, Instant.now());

        ProfileImage image = ProfileImage.builder()
                .userId(userId)
                .fullMediaId(request.fullMediaId())
                .thumbnailMediaId(request.thumbnailMediaId())
                .isActive(true)
                .setAt(Instant.now())
                .build();

        image = profileImageRepository.save(image);

        return new ProfileImageResponse(
                image.getFullMediaId(),
                image.getThumbnailMediaId(),
                image.getSetAt());
    }

    @Transactional
    public void remove(UUID userId) {
        profileImageRepository.deactivateAllForUser(userId, Instant.now());
    }

    private void validateMedia(UUID mediaId, UUID ownerId) {
        Media media = mediaRepository.findByIdAndOwnerId(mediaId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Media", mediaId));

        if ("DELETED".equals(media.getStatus())) {
            throw new ResourceNotFoundException("Media", mediaId);
        }
    }
}