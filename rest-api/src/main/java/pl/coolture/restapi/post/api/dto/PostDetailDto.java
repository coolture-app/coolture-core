package pl.coolture.restapi.post.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import pl.coolture.restapi.dictionary.api.dto.EventCategoryDto;
import pl.coolture.restapi.media.api.dto.MediaResourceDto;
import pl.coolture.restapi.participation.domain.ParticipationType;
import pl.coolture.restapi.post.domain.PostStatus;
import pl.coolture.restapi.post.domain.PostType;
import pl.coolture.restapi.post.domain.PostVisibility;
import pl.coolture.restapi.reaction.domain.ReactionType;
import pl.coolture.restapi.user.api.dto.UserSummaryDto;

/**
 * Full post representation with the complete media list.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostDetailDto {
    private final UUID id;
    private final UserSummaryDto author;
    private final EventCategoryDto category;
    private final EventLocationDto location;
    private final String title;
    private final String description;
    private final String eventUrl;
    private final Instant startsAt;
    private final Instant endsAt;
    private final List<String> tags;
    private final int positiveReactionCount;
    private final int negativeReactionCount;
    private final int participantCount;
    private final int commentsCount;
    private final PostType type;
    private final PostStatus status;
    private final PostVisibility visibility;
    private final Instant createdAt;
    private final Instant lastModifiedAt;
    private final Instant deletedAt;
    private final MediaResourceDto coverMedia;
    private final List<PostMediaDto> media;

    @Setter
    private ReactionType myReaction;
    @Setter
    private ParticipationType myParticipation;
}