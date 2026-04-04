package pl.coolture.restapi.media;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
public class MediaController {

  private final ImageUploadUseCase imageUploadUseCase;
  private final ImageRetrieveUseCase imageRetrieveUseCase;
  private final ImageDeleteUseCase imageDeleteUseCase;

  @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ImageUploadResponse getUploadUrl(@RequestParam("file") MultipartFile file) {
    return imageUploadUseCase.uploadImage(file);
  }

  @PostMapping("/images")
  public ResponseEntity<List<ImageRetrieveResponse>> getImageUrls(@RequestBody List<UUID> ids) {
    List<ImageRetrieveResponse> responses = imageRetrieveUseCase.getImageUrls(ids);
    return ResponseEntity.ok(responses);
  }

  @DeleteMapping("/image/{id}")
  public ResponseEntity<Void> deleteImage(@PathVariable UUID id) {
    imageDeleteUseCase.deleteImage(id);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/images")
  public ResponseEntity<Void> deleteImages(@RequestBody List<UUID> ids) {
    imageDeleteUseCase.deleteImages(ids);
    return ResponseEntity.noContent().build();
  }
}
