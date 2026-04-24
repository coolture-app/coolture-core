package pl.coolture.restapi.media.api;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.coolture.restapi.common.mapper.BaseMapperConfig;
import pl.coolture.restapi.media.api.dto.MediaResourceDto;
import pl.coolture.restapi.media.domain.Media;

@Mapper(config = BaseMapperConfig.class)
public interface MediaMapper {

    /**
     * The presigned GET URL is passed explicitly
     * because it must be generated at call time
     * it cannot be derived from entity fields alone.
     */
    @Mapping(target = "url", source = "url")
    MediaResourceDto toDto(Media media, String url);
}