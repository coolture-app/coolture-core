package pl.coolture.restapi.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.coolture.restapi.common.exceptions.ForbiddenException;
import pl.coolture.restapi.common.exceptions.ResourceNotFoundException;
import pl.coolture.restapi.common.pagination.CursorPage;
import pl.coolture.restapi.common.pagination.PaginationMeta;
import pl.coolture.restapi.common.security.ControllerTestWithSecurity;
import pl.coolture.restapi.user.api.UserController;
import pl.coolture.restapi.user.api.dto.UserProfileUpdateRequest;
import pl.coolture.restapi.user.api.dto.UserSummaryDto;
import pl.coolture.restapi.user.application.UserProvisioningService;
import pl.coolture.restapi.user.application.UserService;

@ControllerTestWithSecurity(UserController.class)
class UserControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean UserService userService;
    @MockitoBean UserProvisioningService userProvisioningService;

    private static final String USERS = "/users";
    private static final String FAKE_USERNAME = "ziomek123";
    private static final String FAKE_FIRST_NAME = "Kunegunda";
    private static final String FAKE_LAST_NAME = "Kowalaska";

    @Test
    void search_returns401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get(USERS))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void search_returns200WithPage_whenAuthenticated() throws Exception {
        var summary = summary(UUID.randomUUID(), FAKE_USERNAME, FAKE_FIRST_NAME, FAKE_LAST_NAME);
        var page    = pageOf(List.of(summary), false);

        when(userService.search(eq(null), eq(null), eq(20))).thenReturn(page);

        mockMvc.perform(get(USERS).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].username").value(FAKE_USERNAME))
                .andExpect(jsonPath("$.items[0].firstName").value(FAKE_FIRST_NAME))
                .andExpect(jsonPath("$.items[0].lastName").value(FAKE_LAST_NAME))
                .andExpect(jsonPath("$.page.hasMore").value(false));
    }

    @Test
    void search_passesQueryParam() throws Exception {
        String query = "someone";

        when(userService.search(eq(query), any(), eq(20))).thenReturn(pageOf(List.of(), false));

        mockMvc.perform(get(USERS).param("q", query).with(jwt()))
                .andExpect(status().isOk());
    }

    @Test
    void search_returns400_whenLimitExceedsMax() throws Exception {
        // max is 100
        mockMvc.perform(get(USERS).param("limit", "999").with(jwt()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getById_returns200_whenUserExists() throws Exception {
        UUID id = UUID.randomUUID();
        UUID caller  = UUID.randomUUID();
        var  profile = AuthControllerTest.profile(id, FAKE_USERNAME, FAKE_FIRST_NAME, FAKE_LAST_NAME);

        when(userService.getByIdForCaller(id, caller)).thenReturn(profile);

        mockMvc.perform(get(USERS + "/{id}", id)
                        .with(jwt().jwt(j -> j.subject(caller.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.username").value(FAKE_USERNAME));
    }

    @Test
    void getById_returns404_whenUserMissing() throws Exception {
        UUID id = UUID.randomUUID();
        UUID caller = UUID.randomUUID();
        when(userService.getByIdForCaller(id, caller))
                .thenThrow(new ResourceNotFoundException("User", id));

        mockMvc.perform(get(USERS + "/{id}", id)
                        .with(jwt().jwt(j -> j.subject(caller.toString()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void getByUsername_returns200_whenFound() throws Exception {
        UUID id = UUID.randomUUID();
        UUID caller = UUID.randomUUID();
        var  profile = AuthControllerTest.profile(id, FAKE_USERNAME, FAKE_FIRST_NAME, FAKE_LAST_NAME);

        when(userService.getByUsernameForCaller(FAKE_USERNAME, caller)).thenReturn(profile);

        mockMvc.perform(get(USERS + "/by-username/{username}", FAKE_USERNAME)
                        .with(jwt().jwt(j -> j.subject(caller.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(FAKE_USERNAME));
    }

    @Test
    void getByUsername_returns404_whenNotFound() throws Exception {
        UUID caller = UUID.randomUUID();
        when(userService.getByUsernameForCaller(FAKE_USERNAME, caller))
                .thenThrow(new ResourceNotFoundException("User", FAKE_USERNAME));

        mockMvc.perform(get(USERS + "/by-username/{username}", FAKE_USERNAME)
                        .with(jwt().jwt(j -> j.subject(caller.toString()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_returns401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(patch(USERS + "/{id}", UUID.randomUUID())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"username":"new_name"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void update_returns200_whenUpdatingOwnProfile() throws Exception {
        UUID id = UUID.randomUUID();
        var updated = AuthControllerTest.profile(id, "new_name", FAKE_FIRST_NAME, FAKE_LAST_NAME);

        when(userService.update(eq(id), eq(id), any(UserProfileUpdateRequest.class)))
                .thenReturn(updated);

        mockMvc.perform(patch(USERS + "/{id}", id)
                        .with(jwt().jwt(j -> j.subject(id.toString())))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"username":"new_name"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("new_name"));
    }

    @Test
    void update_returns403_whenUpdatingAnotherUsersProfile() throws Exception {
        UUID targetId = UUID.randomUUID();
        UUID callerId = UUID.randomUUID();

        when(userService.update(eq(targetId), eq(callerId), any()))
                .thenThrow(new ForbiddenException("You can only update your own profile"));

        mockMvc.perform(patch(USERS + "/{id}", targetId)
                        .with(jwt().jwt(j -> j.subject(callerId.toString())))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"username":"hacked"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void update_returns400_whenUsernameExceedsMaxLength() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(patch(USERS + "/{id}", id)
                        .with(jwt().jwt(j -> j.subject(id.toString())))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"username":"%s"}
                                """.formatted("x".repeat(33))))
                .andExpect(status().isBadRequest());
    }

    private static UserSummaryDto summary(UUID id, String username, String firstName, String lastName) {
        return new UserSummaryDto(id, username, firstName, lastName, null, Instant.now(), 0, 0);
    }

    private static CursorPage<UserSummaryDto> pageOf(List<UserSummaryDto> items, boolean hasMore) {
        var meta = PaginationMeta.builder().limit(20).hasMore(hasMore).build();
        return new CursorPage<>(items, meta);
    }
}