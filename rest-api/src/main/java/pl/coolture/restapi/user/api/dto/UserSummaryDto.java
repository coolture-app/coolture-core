package pl.coolture.restapi.user.api.dto;

import java.time.Instant;
import java.util.UUID;
import pl.coolture.restapi.media.api.dto.MediaResourceDto;

/**
 * Compact user representation used in lists, feed cards, and comment authors.
 */
public record UserSummaryDto(
        UUID id,
        String username,
        String firstName,
        String lastName,
        MediaResourceDto avatar,
        Instant createdAt,
        int followersCount,
        int followingCount) {}