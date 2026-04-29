package pl.coolture.restapi.comment.api;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;
import pl.coolture.restapi.comment.api.dto.CommentSummaryDto;
import pl.coolture.restapi.comment.domain.Comment;
import pl.coolture.restapi.common.mapper.BaseMapperConfig;
import pl.coolture.restapi.media.api.dto.MediaResourceDto;
import pl.coolture.restapi.user.api.UserMapper;

@Mapper(
        config = BaseMapperConfig.class,
        uses   = {UserMapper.class}
)
public abstract class CommentMapper {

    @Autowired
    protected UserMapper userMapper;

    @Mapping(target = "postId",          source = "c.post.id")
    @Mapping(target = "rootCommentId",   source = "c.rootComment.id")
    @Mapping(target = "parentCommentId", source = "c.parentComment.id")
    @Mapping(target = "id",              source = "c.id")
    @Mapping(target = "createdAt",       source = "c.createdAt")
    @Mapping(target = "deletedAt",       source = "c.deletedAt")
    @Mapping(target = "status",          source = "c.status")
    @Mapping(target = "author",
            expression = "java(userMapper.toSummaryDto(c.getAuthor(), authorAvatar))")
    @Mapping(target = "depth",
            expression = "java(c.getAncestorIds() == null ? 0 : c.getAncestorIds().length)")
    public abstract CommentSummaryDto toDto(Comment c, MediaResourceDto authorAvatar);
}