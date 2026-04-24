package pl.coolture.restapi.post.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import pl.coolture.restapi.dictionary.api.dto.EventCategoryDto;
import pl.coolture.restapi.media.api.dto.MediaResourceDto;
import pl.coolture.restapi.user.api.dto.UserSummaryDto;

/**
 * Full post representation with the complete media list.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PostDetailDto(
        UUID id,
        UserSummaryDto author,
        EventCategoryDto category,
        EventLocationDto location,
        String title,
        String description,
        String eventUrl,
        Instant startsAt,
        Instant endsAt,
        List<String> tags,
        int positiveReactionCount,
        int negativeReactionCount,
        int participantCount,
        int commentsCount,
        String type,
        String status,
        String visibility,
        Instant createdAt,
        Instant lastModifiedAt,
        Instant deletedAt,
        String myReaction,
        String myParticipation,
        MediaResourceDto coverMedia,
        List<PostMediaDto> media) {}