package pl.coolture.restapi.post.api.dto;

import pl.coolture.restapi.media.api.dto.MediaResourceDto;

public record PostMediaDto(
        MediaResourceDto media,
        int position,
        boolean isCover) {}