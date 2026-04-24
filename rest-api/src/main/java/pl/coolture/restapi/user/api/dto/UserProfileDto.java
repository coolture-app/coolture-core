package pl.coolture.restapi.user.api.dto;

import java.time.Instant;
import java.util.UUID;
import pl.coolture.restapi.media.api.dto.MediaResourceDto;

/**
 * Full user profile
 */
public record UserProfileDto(
        UUID id,
        String username,
        String firstName,
        String lastName,
        MediaResourceDto avatar,
        int followersCount,
        int followingCount,
        String bio,
        Instant createdAt,
        boolean isFollowing,
        boolean isBlocked) {}