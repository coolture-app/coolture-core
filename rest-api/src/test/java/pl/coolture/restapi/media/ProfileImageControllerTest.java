package pl.coolture.restapi.media;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.coolture.restapi.common.exceptions.ResourceNotFoundException;
import pl.coolture.restapi.common.security.ControllerTestWithSecurity;
import pl.coolture.restapi.media.api.ProfileImageController;
import pl.coolture.restapi.media.api.dto.ProfileImageResponse;
import pl.coolture.restapi.media.application.ProfileImageService;
import pl.coolture.restapi.user.application.UserProvisioningService;

@ControllerTestWithSecurity(ProfileImageController.class)
class ProfileImageControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean ProfileImageService profileImageService;
    @MockitoBean UserProvisioningService userProvisioningService;

    private static final UUID USER_ID       = UUID.randomUUID();
    private static final UUID FULL_ID       = UUID.randomUUID();
    private static final UUID THUMBNAIL_ID  = UUID.randomUUID();
    private static final UUID OTHER_USER_ID = UUID.randomUUID();

    @Test
    void set_returns401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(put("/users/{id}/profile-image", USER_ID)
                        .contentType(APPLICATION_JSON)
                        .content(validSetBody()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void set_returns200_whenSettingOwnImage() throws Exception {
        var response = new ProfileImageResponse(FULL_ID, THUMBNAIL_ID, Instant.now());
        when(profileImageService.set(eq(USER_ID), any())).thenReturn(response);

        mockMvc.perform(put("/users/{id}/profile-image", USER_ID)
                        .with(jwt().jwt(j -> j.subject(USER_ID.toString())))
                        .contentType(APPLICATION_JSON)
                        .content(validSetBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullMediaId").value(FULL_ID.toString()))
                .andExpect(jsonPath("$.thumbnailMediaId").value(THUMBNAIL_ID.toString()))
                .andExpect(jsonPath("$.setAt").exists());
    }

    @Test
    void set_returns403_whenSettingAnotherUsersImage() throws Exception {
        mockMvc.perform(put("/users/{id}/profile-image", USER_ID)
                        .with(jwt().jwt(j -> j.subject(OTHER_USER_ID.toString())))
                        .contentType(APPLICATION_JSON)
                        .content(validSetBody()))
                .andExpect(status().isForbidden());
    }

    @Test
    void set_returns400_whenBodyMissingFields() throws Exception {
        mockMvc.perform(put("/users/{id}/profile-image", USER_ID)
                        .with(jwt().jwt(j -> j.subject(USER_ID.toString())))
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void set_returns404_whenMediaNotFound() throws Exception {
        when(profileImageService.set(any(), any()))
                .thenThrow(new ResourceNotFoundException("Media", FULL_ID));

        mockMvc.perform(put("/users/{id}/profile-image", USER_ID)
                        .with(jwt().jwt(j -> j.subject(USER_ID.toString())))
                        .contentType(APPLICATION_JSON)
                        .content(validSetBody()))
                .andExpect(status().isNotFound());
    }

    @Test
    void remove_returns401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(delete("/users/{id}/profile-image", USER_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void remove_returns204_whenRemovingOwnImage() throws Exception {
        mockMvc.perform(delete("/users/{id}/profile-image", USER_ID)
                        .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
                .andExpect(status().isNoContent());

        verify(profileImageService).remove(USER_ID);
    }

    @Test
    void remove_returns403_whenRemovingAnotherUsersImage() throws Exception {
        mockMvc.perform(delete("/users/{id}/profile-image", USER_ID)
                        .with(jwt().jwt(j -> j.subject(OTHER_USER_ID.toString()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void remove_returns204_whenNoImageIsActive_idempotent() throws Exception {
        mockMvc.perform(delete("/users/{id}/profile-image", USER_ID)
                        .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
                .andExpect(status().isNoContent());
    }

    private String validSetBody() {
        return """
                {
                  "fullMediaId": "%s",
                  "thumbnailMediaId": "%s"
                }
                """.formatted(FULL_ID, THUMBNAIL_ID);
    }
}