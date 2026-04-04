package pl.coolture.restapi.media;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.coolture.restapi.common.config.storage.S3Properties;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageRetrieveUseCase {

  private final ImageRepository imageRepository;
  private final S3Presigner s3Presigner;
  private final S3Properties s3Properties;

  private static final Duration URL_EXPIRATION = Duration.ofMinutes(15);

  public List<ImageRetrieveResponse> getImageUrls(List<UUID> ids) {
    List<ImageEntity> images = imageRepository.findAllById(ids);

    if (images.size() != ids.size()) {
      List<UUID> found = images.stream().map(ImageEntity::getId).toList();
      List<UUID> missing = ids.stream().filter(id -> !found.contains(id)).toList();
      throw new IllegalArgumentException("Images not found for IDs: " + missing);
    }

    return images.stream().map(this::toPresignedUrlResponse).collect(Collectors.toList());
  }

  private ImageRetrieveResponse toPresignedUrlResponse(ImageEntity image) {
    GetObjectRequest getObjectRequest =
        GetObjectRequest.builder()
            .bucket(s3Properties.getBucketName())
            .key(image.getS3Key())
            .build();

    GetObjectPresignRequest presignRequest =
        GetObjectPresignRequest.builder()
            .signatureDuration(URL_EXPIRATION)
            .getObjectRequest(getObjectRequest)
            .build();

    PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
    String url = presignedRequest.url().toString();

    return new ImageRetrieveResponse(image.getId(), image.getOwnerId(), image.getS3Key(), url);
  }
}
