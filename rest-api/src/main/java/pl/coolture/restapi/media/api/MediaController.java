package pl.coolture.restapi.media.api;

import jakarta.validation.Valid;

import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pl.coolture.restapi.media.api.dto.MediaCompleteRequest;
import pl.coolture.restapi.media.api.dto.MediaResourceDto;
import pl.coolture.restapi.media.api.dto.MediaUploadInitRequest;
import pl.coolture.restapi.media.api.dto.MediaUploadInitResponse;
import pl.coolture.restapi.media.domain.MediaPurpose;
import pl.coolture.restapi.media.application.MediaService;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @PostMapping("/uploads/init")
    public ResponseEntity<MediaUploadInitResponse> initUpload(
            @Valid @RequestBody MediaUploadInitRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID callerId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mediaService.initUpload(callerId, request));
    }

    @PostMapping("/uploads/{mediaId}/complete")
    public ResponseEntity<MediaResourceDto> completeUpload(
            @PathVariable UUID mediaId,
            @RequestBody(required = false) MediaCompleteRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID callerId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(mediaService.completeUpload(mediaId, callerId, request));
    }

    @GetMapping("/{mediaId}")
    public ResponseEntity<MediaResourceDto> getById(@PathVariable UUID mediaId) {
        return ResponseEntity.ok(mediaService.getById(mediaId));
    }

    @DeleteMapping("/{mediaId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID mediaId,
            @AuthenticationPrincipal Jwt jwt) {

        UUID callerId = UUID.fromString(jwt.getSubject());
        mediaService.delete(mediaId, callerId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Endpoint only for admin usage in Swagger
     * performs init + S3 PUT + complete in one request.
     * Accepts multipart/form-data so Swagger UI can attach a file from disk.
     *
     * Intended for testing
     */
    @PreAuthorize("hasRole('COOLTURE_ADMIN')")
    @PostMapping(value = "/admin/uploads/direct", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaResourceDto> directUpload(
            @RequestParam MediaPurpose purpose,
            @RequestParam MultipartFile file,
            @AuthenticationPrincipal Jwt jwt) throws IOException {

        UUID callerId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mediaService.directUpload(callerId, purpose, file));
    }
}