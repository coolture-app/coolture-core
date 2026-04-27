package pl.coolture.restapi.comment.api;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.coolture.restapi.comment.api.dto.CommentSummaryDto;
import pl.coolture.restapi.comment.domain.Comment;
import pl.coolture.restapi.common.mapper.BaseMapperConfig;
import pl.coolture.restapi.user.api.UserMapper;

@Mapper(
        config = BaseMapperConfig.class,
        uses   = {UserMapper.class}
)
public abstract class CommentMapper {

    /**
     * MapStruct walks the lazy associations only as far as their primary keys,
     * so this does not trigger SELECTs on root_comment / parent_comment / post.
     *
     * `depth` is computed from ancestor_ids length:
     *   null/empty -> 0 (root)
     *   length 1   -> 1 (first reply)
     *   length 2   -> 2 (reply to reply, max allowed)
     */
    @Mapping(target = "postId",          source = "post.id")
    @Mapping(target = "rootCommentId",   source = "rootComment.id")
    @Mapping(target = "parentCommentId", source = "parentComment.id")
    @Mapping(target = "depth",
            expression = "java(c.getAncestorIds() == null ? 0 : c.getAncestorIds().length)")
    public abstract CommentSummaryDto toDto(Comment c);
}