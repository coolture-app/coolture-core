package pl.coolture.restapi.user;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.coolture.restapi.common.security.ControllerTestWithSecurity;
import pl.coolture.restapi.user.api.AuthController;
import pl.coolture.restapi.user.api.dto.UserProfileDto;
import pl.coolture.restapi.user.application.UserProvisioningService;
import pl.coolture.restapi.user.application.UserService;

@ControllerTestWithSecurity(AuthController.class)
class AuthControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean UserService userService;
    @MockitoBean UserProvisioningService userProvisioningService;

    private static final String ME = "/auth/me";

    @Test
    void me_returns401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get(ME))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_returns200WithProfile_whenAuthenticated() throws Exception {
        UUID userId = UUID.randomUUID();
        String username = "some_username";
        String firstName = "Jan";
        String lastName = "Kowalski";

        var profile = profile(userId, username, firstName, lastName);

        when(userService.getById(userId)).thenReturn(profile);

        mockMvc.perform(get(ME).with(jwt().jwt(j -> j.subject(userId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.firstName").value(firstName))
                .andExpect(jsonPath("$.lastName").value(lastName));
    }

    static UserProfileDto profile(UUID id, String username, String firstName, String lastName) {
        return new UserProfileDto(id, username, firstName, lastName,
                null, 0, 0, "bio", Instant.now(), false, false);
    }
}