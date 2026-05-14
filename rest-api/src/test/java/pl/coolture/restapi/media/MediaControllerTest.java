package pl.coolture.restapi.media;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.coolture.restapi.common.exceptions.ConflictException;
import pl.coolture.restapi.common.exceptions.ForbiddenException;
import pl.coolture.restapi.common.exceptions.ResourceNotFoundException;
import pl.coolture.restapi.common.security.ControllerTestWithSecurity;
import pl.coolture.restapi.media.api.MediaController;
import pl.coolture.restapi.media.api.dto.MediaResourceDto;
import pl.coolture.restapi.media.api.dto.MediaUploadInitResponse;
import pl.coolture.restapi.media.application.MediaService;
import pl.coolture.restapi.media.domain.MediaPurpose;
import pl.coolture.restapi.media.domain.MediaStatus;
import pl.coolture.restapi.user.application.UserProvisioningService;

@ControllerTestWithSecurity(MediaController.class)
class MediaControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean MediaService mediaService;
    @MockitoBean UserProvisioningService userProvisioningService;

    private static final UUID MEDIA_ID  = UUID.randomUUID();
    private static final UUID CALLER_ID = UUID.randomUUID();

    @Test
    void initUpload_returns401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/media/uploads/init")
                        .contentType(APPLICATION_JSON)
                        .content(validInitBody()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void initUpload_returns201_withPresignedUrl() throws Exception {
        var response = new MediaUploadInitResponse(
                MEDIA_ID, "profile_image/id/file.jpg",
                "http://localhost:8080/bucket/key?sig=abc",
                "PUT", Instant.now().plusSeconds(900),
                Map.of("Content-Type", "image/jpeg"));

        when(mediaService.initUpload(eq(CALLER_ID), any())).thenReturn(response);

        mockMvc.perform(post("/media/uploads/init")
                        .with(jwt().jwt(j -> j.subject(CALLER_ID.toString())))
                        .contentType(APPLICATION_JSON)
                        .content(validInitBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mediaId").value(MEDIA_ID.toString()))
                .andExpect(jsonPath("$.httpMethod").value("PUT"))
                .andExpect(jsonPath("$.uploadUrl").exists());
    }

    @Test
    void initUpload_returns400_whenBodyInvalid() throws Exception {
        mockMvc.perform(post("/media/uploads/init")
                        .with(jwt())
                        .contentType(APPLICATION_JSON)
                        .content("{}"))  // missing required fields
                .andExpect(status().isBadRequest());
    }

    @Test
    void completeUpload_returns401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/media/uploads/{id}/complete", MEDIA_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void completeUpload_returns200_withMediaDto() throws Exception {
        var dto = mediaDto(MEDIA_ID, MediaStatus.UPLOADED);
        when(mediaService.completeUpload(eq(MEDIA_ID), eq(CALLER_ID), any()))
                .thenReturn(dto);

        mockMvc.perform(post("/media/uploads/{id}/complete", MEDIA_ID)
                        .with(jwt().jwt(j -> j.subject(CALLER_ID.toString())))
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(MEDIA_ID.toString()))
                .andExpect(jsonPath("$.status").value("UPLOADED"));
    }

    @Test
    void completeUpload_returns409_whenAlreadyCompleted() throws Exception {
        when(mediaService.completeUpload(any(), any(), any()))
                .thenThrow(new ConflictException("Media upload is already completed or invalid"));

        mockMvc.perform(post("/media/uploads/{id}/complete", MEDIA_ID)
                        .with(jwt().jwt(j -> j.subject(CALLER_ID.toString())))
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict());
    }

    @Test
    void getById_returns200_whenFound() throws Exception {
        when(mediaService.getById(MEDIA_ID)).thenReturn(mediaDto(MEDIA_ID, MediaStatus.UPLOADED));

        mockMvc.perform(get("/media/{id}", MEDIA_ID).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(MEDIA_ID.toString()));
    }

    @Test
    void getById_returns404_whenNotFound() throws Exception {
        when(mediaService.getById(MEDIA_ID))
                .thenThrow(new ResourceNotFoundException("Media", MEDIA_ID));

        mockMvc.perform(get("/media/{id}", MEDIA_ID).with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_returns401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(delete("/media/{id}", MEDIA_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void delete_returns204_whenSuccessful() throws Exception {
        mockMvc.perform(delete("/media/{id}", MEDIA_ID)
                        .with(jwt().jwt(j -> j.subject(CALLER_ID.toString()))))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_returns409_whenMediaInUse() throws Exception {
        doThrow(new ConflictException("Media is currently in use and cannot be deleted"))
                .when(mediaService).delete(any(), any());

        mockMvc.perform(delete("/media/{id}", MEDIA_ID)
                        .with(jwt().jwt(j -> j.subject(CALLER_ID.toString()))))
                .andExpect(status().isConflict());
    }

    @Test
    void delete_returns403_whenNotOwner() throws Exception {
        doThrow(new ForbiddenException("You do not own this media"))
                .when(mediaService).delete(any(), any());

        mockMvc.perform(delete("/media/{id}", MEDIA_ID)
                        .with(jwt().jwt(j -> j.subject(CALLER_ID.toString()))))
                .andExpect(status().isForbidden());
    }

    private static String validInitBody() {
        return """
                {
                  "purpose": "EVENT_MEDIA",
                  "mimeType": "image/jpeg",
                  "sizeBytes": 204800,
                  "fileName": "photo.jpg"
                }
                """;
    }

    static MediaResourceDto mediaDto(UUID id, MediaStatus status) {
        return new MediaResourceDto(id, MediaPurpose.EVENT_MEDIA, "image/jpeg",
                204800L, status, "http://localhost:8080/bucket/key?sig=abc",
                Instant.now(), null);
    }
}