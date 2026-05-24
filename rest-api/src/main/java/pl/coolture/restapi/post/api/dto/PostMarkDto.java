package pl.coolture.restapi.post.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class PostMarkDto {
    private final UUID id;
    private final String title;
    private final String desc;
    private final String coverMediaUrl;
    private final int positiveReactionCount;
    private final GeoPointDto coordinates;
}
