package pl.coolture.restapi.media.application;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pl.coolture.restapi.common.exceptions.ConflictException;
import pl.coolture.restapi.common.exceptions.ForbiddenException;
import pl.coolture.restapi.common.exceptions.ResourceNotFoundException;
import pl.coolture.restapi.media.api.MediaMapper;
import pl.coolture.restapi.media.api.dto.MediaCompleteRequest;
import pl.coolture.restapi.media.api.dto.MediaResourceDto;
import pl.coolture.restapi.media.api.dto.MediaUploadInitRequest;
import pl.coolture.restapi.media.api.dto.MediaUploadInitResponse;
import pl.coolture.restapi.media.domain.Media;
import pl.coolture.restapi.media.domain.MediaPurpose;
import pl.coolture.restapi.media.domain.MediaStatus;
import pl.coolture.restapi.media.domain.MediaRepository;
import pl.coolture.restapi.common.config.storage.PresignService;
import pl.coolture.restapi.common.config.storage.S3Properties;
import pl.coolture.restapi.media.domain.MediaMimeType;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MediaService {

    private static final MediaStatus STATUS_PENDING  = MediaStatus.PENDING;
    private static final MediaStatus STATUS_UPLOADED = MediaStatus.UPLOADED;
    private static final MediaStatus STATUS_DELETED  = MediaStatus.DELETED;

    private final MediaRepository  mediaRepository;
    private final PresignService   presignService;
    private final S3Client         s3Client;
    private final S3Properties     s3Properties;
    private final MediaMapper      mediaMapper;

    @Transactional
    public MediaUploadInitResponse initUpload(UUID ownerId, MediaUploadInitRequest request) {
        MediaMimeType.validate(request.mimeType());

        String ext = extractExtension(request.fileName());
        String objectKey = "%s/%s/%s.%s".formatted(request.purpose(), ownerId, UUID.randomUUID(), ext);

        Media media = Media.builder()
                .ownerId(ownerId)
                .objectKey(objectKey)
                .fileName(request.fileName())
                .purpose(request.purpose())
                .mimeType(request.mimeType())
                .sizeBytes(request.sizeBytes())
                .status(STATUS_PENDING)
                .createdAt(Instant.now())
                .build();

        media = mediaRepository.save(media);

        PresignedPutObjectRequest presigned =
                presignService.presignPut(objectKey, request.mimeType(), request.sizeBytes());

        // Required headers that the client must include in the PUT request.
        // Content-Type is locked into the signature - omitting it breaks the upload.
        Map<String, String> requiredHeaders = presigned.signedHeaders().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        e -> String.join(",", e.getValue())));

        return new MediaUploadInitResponse(
                media.getId(),
                objectKey,
                presigned.url().toString(),
                "PUT",
                presigned.expiration(),
                requiredHeaders);
    }

    @Transactional
    public MediaResourceDto completeUpload(UUID mediaId, UUID callerId, MediaCompleteRequest request) {
        Media media = findOwnedOrThrow(mediaId, callerId);

        if (!STATUS_PENDING.equals(media.getStatus())) {
            throw new ConflictException("Media upload is already completed or invalid");
        }

        // TODO: optionally verify ETag against S3 HeadObject response
        //  to confirm the file was not corrupted in transit.

        media.setStatus(STATUS_UPLOADED);
        return toDto(media);
    }

    public MediaResourceDto getById(UUID mediaId) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media", mediaId));

        if (STATUS_DELETED.equals(media.getStatus())) {
            throw new ResourceNotFoundException("Media", mediaId);
        }

        return toDto(media);
    }

    @Transactional
    public void delete(UUID mediaId, UUID callerId) {
        Media media = findOwnedOrThrow(mediaId, callerId);

        if (STATUS_DELETED.equals(media.getStatus())) {
            throw new ConflictException("Media is already deleted");
        }
        if ("ATTACHED".equals(media.getStatus())) {
            throw new ConflictException("Media is currently in use and cannot be deleted");
        }

        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(s3Properties.getBucketName())
                .key(media.getObjectKey())
                .build());

        media.setStatus(STATUS_DELETED);
        media.setDeletedAt(Instant.now());
    }

    /**
     * Generates a fresh presigned GET URL and maps the entity to a DTO.
     *
     * TODO: Cache the presigned GET URL with Caffeine
     */
    public MediaResourceDto toDto(Media media) {
        String url = presignService.presignGet(media.getObjectKey()).url().toString();
        return mediaMapper.toDto(media, url);
    }

    private Media findOwnedOrThrow(UUID mediaId, UUID callerId) {
        return mediaRepository.findByIdAndOwnerId(mediaId, callerId)
                .orElseThrow(() -> {
                    if (mediaRepository.existsById(mediaId)) {
                        return new ForbiddenException("You do not own this media");
                    }
                    return new ResourceNotFoundException("Media", mediaId);
                });
    }

    private static String extractExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return (dot >= 0 && dot < fileName.length() - 1)
                ? fileName.substring(dot + 1).toLowerCase()
                : "bin";
    }

    /**
     * Uploads a file directly to S3 server-side and returns the completed media record.
     * Used by the /media/uploads/direct convenience endpoint.
     */
    @Transactional
    public MediaResourceDto directUpload(UUID ownerId, MediaPurpose purpose, MultipartFile file) throws IOException {
        String ext = extractExtension(
                Optional.ofNullable(file.getOriginalFilename()).orElse("file.bin"));
        String objectKey = "%s/%s/%s.%s".formatted(purpose, ownerId, UUID.randomUUID(), ext);
        String mimeType = Optional.ofNullable(file.getContentType()).orElse("application/octet-stream");
        MediaMimeType.validate(mimeType);

        // Upload bytes directly to S3 using the internal S3Client (not presigned)
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(s3Properties.getBucketName())
                        .key(objectKey)
                        .contentType(mimeType)
                        .contentLength(file.getSize())
                        .build(),
                RequestBody.fromBytes(file.getBytes()));

        Media media = Media.builder()
                .ownerId(ownerId)
                .objectKey(objectKey)
                .fileName(Optional.ofNullable(file.getOriginalFilename()).orElse("upload"))
                .purpose(purpose)
                .mimeType(mimeType)
                .sizeBytes(file.getSize())
                .status(STATUS_UPLOADED)
                .createdAt(Instant.now())
                .build();

        return toDto(mediaRepository.save(media));
    }
}