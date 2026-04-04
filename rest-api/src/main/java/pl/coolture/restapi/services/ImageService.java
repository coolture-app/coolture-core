package pl.coolture.restapi.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageService {
  private static final List<String> ALLOWED_EXTENSIONS = List.of(".jpg", ".jpeg", ".png", ".webp");

  public List<UUID> saveImages(List<MultipartFile> images, String path) {
    List<UUID> savedUuids = new ArrayList<>();

    if (images == null || images.isEmpty()) {
      return savedUuids;
    }

    try {
      // TODO CHANGE TO .ENV
      String uploadDir = "uploads/" + path + "/";
      Path uploadPath = Paths.get(uploadDir);

      if (!Files.exists(uploadPath)) {
        Files.createDirectories(uploadPath);
      }

      for (MultipartFile image : images) {
        if (image.isEmpty()) {
          continue;
        }

        UUID imageUuid = UUID.randomUUID();

        String originalFilename = image.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
          extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
          throw new IllegalArgumentException("File type not allowed: " + extension);
        }
        String newFilename = imageUuid.toString() + extension;
        Path filePath = uploadPath.resolve(newFilename);
        Files.copy(image.getInputStream(), filePath);
        savedUuids.add(imageUuid);
      }
    } catch (IOException e) {
      throw new RuntimeException("Couldn't save photos on disk!", e);
    }
    return savedUuids;
  }

  public Resource getImage(String uuid, String path) {
    try {
      Path uploadPath = Paths.get("uploads/" + path + "/");

      try (Stream<Path> files = Files.list(uploadPath)) {
        Path foundFile =
            files
                .filter(f -> f.getFileName().toString().startsWith(uuid))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Couldn't find the photo " + uuid));

        Resource resource = new UrlResource(foundFile.toUri());
        if (resource.exists() || resource.isReadable()) {
          return resource;
        } else {
          throw new RuntimeException("Cant read the file");
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("ERRRR: ", e);
    }
  }
}
