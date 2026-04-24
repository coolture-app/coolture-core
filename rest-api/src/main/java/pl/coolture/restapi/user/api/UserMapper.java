package pl.coolture.restapi.user.api;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import pl.coolture.restapi.common.mapper.BaseMapperConfig;
import pl.coolture.restapi.user.api.dto.UserProfileDto;
import pl.coolture.restapi.user.api.dto.UserProfileUpdateRequest;
import pl.coolture.restapi.user.api.dto.UserSummaryDto;
import pl.coolture.restapi.user.domain.User;

@Mapper(config = BaseMapperConfig.class)
public interface UserMapper {

    /**
     * isFollowing / isBlocked are context-dependent (caller vs. target)
     * they cannot be derived from the entity alone, so they are set
     * explicitly in UserService after mapping.
     * TODO: do not ignore avatar after media fully implemented
     */
    @Mapping(target = "avatar", ignore = true)
    @Mapping(target = "isFollowing", constant = "false")
    @Mapping(target = "isBlocked", constant = "false")
    UserProfileDto toProfileDto(User user);

    @Mapping(target = "avatar", ignore = true)
    UserSummaryDto toSummaryDto(User user);

    /**
     * Applies non-null fields from the request onto the existing entity.
     */
    @Mapping(target = "id",             ignore = true)
    @Mapping(target = "createdAt",      ignore = true)
    @Mapping(target = "followersCount", ignore = true)
    @Mapping(target = "followingCount", ignore = true)
    void updateEntity(UserProfileUpdateRequest request, @MappingTarget User user);
}