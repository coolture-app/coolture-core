package pl.coolture.restapi.media;

import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pl.coolture.restapi.common.config.storage.S3Properties;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageUploadUseCase {

  private final S3Client s3Client;
  private final S3Properties s3Properties;
  private final ImageRepository imageRepository;

  public ImageUploadResponse execute(MultipartFile file) {
    validateFile(file);

    String s3Key = generateS3Key(file.getOriginalFilename());
    String bucket = s3Properties.getBucketName();

    try {
      PutObjectRequest putRequest =
          PutObjectRequest.builder()
              .bucket(bucket)
              .key(s3Key)
              .contentType(file.getContentType())
              .build();

      s3Client.putObject(putRequest, RequestBody.fromBytes(file.getBytes()));

      ImageEntity entity = new ImageEntity(s3Key);
      ImageEntity saved = imageRepository.save(entity);

      return new ImageUploadResponse(
          saved.getId(), saved.getS3Key(), saved.getOwnerId().toString(), saved.getCreatedAt());

    } catch (IOException e) {
      log.error("Failed to read file bytes", e);
      throw new RuntimeException("Failed to read uploaded file", e);
    } catch (S3Exception e) {
      log.error("S3 upload failed: {}", e.awsErrorDetails().errorMessage());
      throw new RuntimeException("Storage service unavailable", e);
    }
  }

  private void validateFile(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("File cannot be empty");
    }
    String contentType = file.getContentType();
    if (contentType == null || !contentType.startsWith("image/")) {
      throw new IllegalArgumentException("Only image files are allowed");
    }
    // Max size is already enforced by .yml conf
  }

  private String generateS3Key(String originalFilename) {
    String safeName =
        originalFilename != null ? originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_") : "file";
    return "images/" + UUID.randomUUID() + "-" + safeName;
  }
}
