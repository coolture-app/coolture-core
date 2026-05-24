package pl.coolture.restapi.post.api;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;
import pl.coolture.restapi.common.mapper.BaseMapperConfig;
import pl.coolture.restapi.dictionary.api.DictionaryMapper;
import pl.coolture.restapi.media.api.dto.MediaResourceDto;
import pl.coolture.restapi.media.application.MediaService;
import pl.coolture.restapi.post.api.dto.*;
import pl.coolture.restapi.post.domain.EventLocation;
import pl.coolture.restapi.post.domain.Post;
import pl.coolture.restapi.post.domain.PostMedia;
import pl.coolture.restapi.user.api.UserMapper;
import java.util.UUID;

@Mapper(
        config = BaseMapperConfig.class,
        uses = {UserMapper.class, DictionaryMapper.class}
)
public abstract class PostMapper {

    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), 4326);

    @Autowired
    protected MediaService mediaService;

    @Autowired
    protected UserMapper userMapper;

    @Mapping(target = "id",        source = "p.id")
    @Mapping(target = "createdAt", source = "p.createdAt")
    @Mapping(target = "deletedAt",    source = "p.deletedAt")
    @Mapping(target = "status",    source = "p.status")
    @Mapping(target = "myReaction", expression = "java(null)")
    @Mapping(target = "myParticipation", expression = "java(null)")
    @Mapping(target = "coverMedia", expression = "java(extractCoverMedia(p.getMedia()))")
    @Mapping(target = "author", expression = "java(userMapper.toSummaryDto(p.getAuthor(), authorAvatar))")
    public abstract PostCardDto toCard(Post p, MediaResourceDto authorAvatar);

    @Mapping(target = "id", source = "p.id")
    @Mapping(target = "status", source = "p.status")
    @Mapping(target = "createdAt", source = "p.createdAt")
    @Mapping(target = "deletedAt", source = "p.deletedAt")
    @Mapping(target = "myReaction", expression = "java(null)")
    @Mapping(target = "myParticipation", expression = "java(null)")
    @Mapping(target = "coverMedia", expression = "java(extractCoverMedia(p.getMedia()))")
    @Mapping(target = "media", expression = "java(mapMediaList(p.getMedia()))")
    @Mapping(target = "author", expression = "java(userMapper.toSummaryDto(p.getAuthor(), authorAvatar))")
    public abstract PostDetailDto toDetail(Post p, MediaResourceDto authorAvatar);

    @Mapping(target = "coordinates", source = "coordinates")
    public abstract EventLocationDto toLocationDto(EventLocation loc);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "coordinates", source = "coordinates")
    public abstract EventLocation toLocationEntity(EventLocationDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "coordinates", source = "coordinates")
    public abstract void updateLocation(@MappingTarget EventLocation loc, EventLocationDto dto);

    @Mapping(target = "id", source = "p.id")
    @Mapping(target = "title", source = "p.title")
    @Mapping(target = "desc", source = "p.description")
    @Mapping(target = "coverMediaUrl", expression = "java(extractCoverMediaUrl(p.getMedia()))")
    @Mapping(target = "positiveReactionCount", source = "p.positiveReactionCount")
    @Mapping(target = "coordinates", source = "p.location.coordinates")
    public abstract PostMarkDto toPostMarkDto(Post p);

    protected GeoPointDto map(Point c) {
        if (c == null) return null;
        // JTS Point: x = longitude, y = latitude
        return new GeoPointDto(c.getY(), c.getX());
    }

    protected Point map(GeoPointDto g) {
        if (g == null) return null;
        // Coordinate(x, y) = (longitude, latitude)
        Point p = GEOMETRY_FACTORY.createPoint(new Coordinate(g.getLongitude(), g.getLatitude()));
        p.setSRID(4326);
        return p;
    }

    protected List<String> map(String[] tags) {
        return tags == null ? List.of() : Arrays.asList(tags);
    }

    protected List<PostMediaDto> mapMediaList(List<PostMedia> media) {
        if (media == null) return List.of();

        return media.stream()
                .sorted(Comparator.comparingInt(PostMedia::getPosition))
                .map(this::toPostMediaDto)
                .toList();
    }

    protected PostMediaDto toPostMediaDto(PostMedia pm) {
        return new PostMediaDto(
                mediaService.toDto(pm.getMedia()),
                pm.getPosition(),
                pm.isCover()
        );
    }

    protected MediaResourceDto extractCoverMedia(List<PostMedia> media) {
        if (media == null || media.isEmpty()) return null;
        return media.stream()
                .filter(PostMedia::isCover)
                .findFirst()
                .or(() -> media.stream().findFirst())
                .map(pm -> mediaService.toDto(pm.getMedia()))
                .orElse(null);
    }

    protected UUID extractCoverMediaId(List<PostMedia> media) {
        if (media == null || media.isEmpty()) return null;
        return media.stream()
                .filter(PostMedia::isCover)
                .findFirst()
                .or(() -> media.stream().findFirst())
                .map(pm -> pm.getMedia().getId())
                .orElse(null);
    }

    protected String extractCoverMediaUrl(List<PostMedia> media) {
        if (media == null || media.isEmpty()) return null;
        return media.stream()
                .filter(PostMedia::isCover)
                .findFirst()
                .or(() -> media.stream().findFirst())
                .map(pm -> mediaService.toDto(pm.getMedia()))
                .map(MediaResourceDto::url)
                .orElse(null);
    }
}