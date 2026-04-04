package pl.coolture.restapi.media;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.coolture.restapi.common.config.security.SecurityUtils;
import pl.coolture.restapi.common.config.storage.S3Properties;
import pl.coolture.restapi.common.exceptions.ForbiddenException;
import pl.coolture.restapi.common.exceptions.ResourceNotFoundException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageDeleteUseCase {

  private final ImageRepository imageRepository;
  private final S3Client s3Client;
  private final S3Properties s3Properties;

  public void deleteImage(UUID id) {
    ImageEntity image =
        imageRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Image not found with id: " + id));
    checkOwnership(image);
    deleteFromS3(image.getS3Key());
    imageRepository.delete(image);
  }

  public void deleteImages(List<UUID> ids) {
    List<ImageEntity> images = imageRepository.findAllById(ids);

    // Validate that all requested IDs exist
    if (images.size() != ids.size()) {
      Set<UUID> foundIds = images.stream().map(ImageEntity::getId).collect(Collectors.toSet());
      List<UUID> missing = ids.stream().filter(id -> !foundIds.contains(id)).toList();
      throw new ResourceNotFoundException("Images not found: " + missing);
    }

    for (ImageEntity image : images) {
      checkOwnership(image);
    }

    for (ImageEntity image : images) {
      deleteFromS3(image.getS3Key());
      imageRepository.delete(image);
    }
  }

  /** Verify that the current user owns the image or has ADMIN role. */
  private void checkOwnership(ImageEntity image) {
    String currentUserId = SecurityUtils.getCurrentUserId();
    boolean isAdmin = SecurityUtils.isCurrentUserAdmin();
    boolean isOwner = image.getOwnerId().toString().equals(currentUserId);

    if (!isOwner && !isAdmin) {
      throw new ForbiddenException("You are not allowed to delete this image");
    }
  }

  /** Delete the object from S3 bucket. */
  private void deleteFromS3(String s3Key) {
    try {
      DeleteObjectRequest deleteRequest =
          DeleteObjectRequest.builder().bucket(s3Properties.getBucketName()).key(s3Key).build();
      s3Client.deleteObject(deleteRequest);
      log.debug("Deleted S3 object: {}", s3Key);
    } catch (S3Exception e) {
      log.error("Failed to delete S3 object: key={}", s3Key, e);
      throw new RuntimeException("Failed to delete image from storage", e);
    }
  }
}
