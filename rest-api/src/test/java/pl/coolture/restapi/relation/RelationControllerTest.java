package pl.coolture.restapi.relation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.coolture.restapi.common.exceptions.ConflictException;
import pl.coolture.restapi.common.exceptions.ResourceNotFoundException;
import pl.coolture.restapi.common.pagination.CursorPage;
import pl.coolture.restapi.common.pagination.PaginationMeta;
import pl.coolture.restapi.common.security.ControllerTestWithSecurity;
import pl.coolture.restapi.relation.api.RelationController;
import pl.coolture.restapi.relation.application.RelationService;
import pl.coolture.restapi.user.api.dto.UserSummaryDto;
import pl.coolture.restapi.user.application.UserProvisioningService;

@ControllerTestWithSecurity(RelationController.class)
class RelationControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean RelationService         relationService;
    @MockitoBean UserProvisioningService userProvisioningService;

    private static final UUID USER_ID   = UUID.randomUUID();
    private static final UUID CALLER_ID = UUID.randomUUID();

    @Test
    void getFollowers_returns401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/users/{id}/followers", USER_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getFollowers_returns200WithPage() throws Exception {
        var page = pageOf(List.of(summary(UUID.randomUUID(), "follower1")), false);
        when(relationService.getFollowers(eq(USER_ID), any(), eq(20))).thenReturn(page);

        mockMvc.perform(get("/users/{id}/followers", USER_ID).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].username").value("follower1"))
                .andExpect(jsonPath("$.page.hasMore").value(false));
    }

    @Test
    void getFollowers_returns404_whenUserNotFound() throws Exception {
        when(relationService.getFollowers(eq(USER_ID), any(), anyInt()))
                .thenThrow(new ResourceNotFoundException("User", USER_ID));

        mockMvc.perform(get("/users/{id}/followers", USER_ID).with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getFollowing_returns401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/users/{id}/following", USER_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getFollowing_returns200WithPage() throws Exception {
        var page = pageOf(List.of(summary(UUID.randomUUID(), "followed1")), false);
        when(relationService.getFollowing(eq(USER_ID), any(), eq(20))).thenReturn(page);

        mockMvc.perform(get("/users/{id}/following", USER_ID).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].username").value("followed1"));
    }

    @Test
    void getBlocking_returns401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/users/{id}/blocking", USER_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getBlocking_returns403_whenViewingAnotherUsersBlockList() throws Exception {
        mockMvc.perform(get("/users/{id}/blocking", USER_ID)
                        .with(jwt().jwt(j -> j.subject(CALLER_ID.toString()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getBlocking_returns200_whenViewingOwnBlockList() throws Exception {
        var page = pageOf(List.of(summary(UUID.randomUUID(), "blocked1")), false);
        when(relationService.getBlocking(eq(USER_ID), any(), eq(20))).thenReturn(page);

        mockMvc.perform(get("/users/{id}/blocking", USER_ID)
                        .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].username").value("blocked1"));
    }

    @Test
    void follow_returns401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/users/{id}/follow", USER_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void follow_returns204_whenSuccessful() throws Exception {
        mockMvc.perform(post("/users/{id}/follow", USER_ID)
                        .with(jwt().jwt(j -> j.subject(CALLER_ID.toString()))))
                .andExpect(status().isNoContent());

        verify(relationService).follow(CALLER_ID, USER_ID);
    }

    @Test
    void follow_returns409_whenAlreadyFollowing() throws Exception {
        doThrow(new ConflictException("Already following this user"))
                .when(relationService).follow(any(), any());

        mockMvc.perform(post("/users/{id}/follow", USER_ID)
                        .with(jwt().jwt(j -> j.subject(CALLER_ID.toString()))))
                .andExpect(status().isConflict());
    }

    @Test
    void follow_returns409_whenFollowingSelf() throws Exception {
        doThrow(new ConflictException("Cannot follow yourself"))
                .when(relationService).follow(any(), any());

        mockMvc.perform(post("/users/{id}/follow", USER_ID)
                        .with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
                .andExpect(status().isConflict());
    }

    @Test
    void unfollow_returns401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(delete("/users/{id}/follow", USER_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unfollow_returns204_whenSuccessful() throws Exception {
        mockMvc.perform(delete("/users/{id}/follow", USER_ID)
                        .with(jwt().jwt(j -> j.subject(CALLER_ID.toString()))))
                .andExpect(status().isNoContent());

        verify(relationService).unfollow(CALLER_ID, USER_ID);
    }

    @Test
    void unfollow_returns404_whenNotFollowing() throws Exception {
        doThrow(new ResourceNotFoundException("Follow relation", USER_ID))
                .when(relationService).unfollow(any(), any());

        mockMvc.perform(delete("/users/{id}/follow", USER_ID)
                        .with(jwt().jwt(j -> j.subject(CALLER_ID.toString()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void block_returns401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/users/{id}/block", USER_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void block_returns204_whenSuccessful() throws Exception {
        mockMvc.perform(post("/users/{id}/block", USER_ID)
                        .with(jwt().jwt(j -> j.subject(CALLER_ID.toString()))))
                .andExpect(status().isNoContent());

        verify(relationService).block(CALLER_ID, USER_ID);
    }

    @Test
    void block_returns409_whenAlreadyBlocking() throws Exception {
        doThrow(new ConflictException("Already blocking this user"))
                .when(relationService).block(any(), any());

        mockMvc.perform(post("/users/{id}/block", USER_ID)
                        .with(jwt().jwt(j -> j.subject(CALLER_ID.toString()))))
                .andExpect(status().isConflict());
    }

    @Test
    void unblock_returns401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(delete("/users/{id}/block", USER_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unblock_returns204_whenSuccessful() throws Exception {
        mockMvc.perform(delete("/users/{id}/block", USER_ID)
                        .with(jwt().jwt(j -> j.subject(CALLER_ID.toString()))))
                .andExpect(status().isNoContent());

        verify(relationService).unblock(CALLER_ID, USER_ID);
    }

    @Test
    void unblock_returns404_whenNotBlocking() throws Exception {
        doThrow(new ResourceNotFoundException("Block relation", USER_ID))
                .when(relationService).unblock(any(), any());

        mockMvc.perform(delete("/users/{id}/block", USER_ID)
                        .with(jwt().jwt(j -> j.subject(CALLER_ID.toString()))))
                .andExpect(status().isNotFound());
    }

    private static UserSummaryDto summary(UUID id, String username) {
        return new UserSummaryDto(id, username, "First", "Last", null, Instant.now(), 0, 0);
    }

    private static CursorPage<UserSummaryDto> pageOf(List<UserSummaryDto> items, boolean hasMore) {
        return new CursorPage<>(items, PaginationMeta.builder().limit(20).hasMore(hasMore).build());
    }
}