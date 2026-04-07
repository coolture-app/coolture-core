package pl.coolture.restapi.services;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.coolture.restapi.dtos.Post.InteractionDTO;
import pl.coolture.restapi.dtos.Post.PostCommentDTO;
import pl.coolture.restapi.exceptionHandlers.exceptionTypes.NotFoundException;
import pl.coolture.restapi.models.Post;
import pl.coolture.restapi.models.PostComment;
import pl.coolture.restapi.models.PostInteraction;
import pl.coolture.restapi.models.User;
import pl.coolture.restapi.repositories.PostCommentRepository;
import pl.coolture.restapi.repositories.PostInteractionRepository;
import pl.coolture.restapi.repositories.PostRepository;
import pl.coolture.restapi.repositories.UserRepository;
import pl.coolture.restapi.utils.InteractionChangeInfo;

@Service
@RequiredArgsConstructor
public class InteractionService {
  private final PostRepository postRepository;
  private final UserRepository userRepository;
  private final PostInteractionRepository postInteractionRepository;
  private final PostCommentRepository postCommentRepository;

  @Transactional
  public void changeInteractionState(InteractionDTO interactionDTO, InteractionChangeInfo info) {
    UUID userUuid = interactionDTO.getUserUuid();
    UUID postUuid = interactionDTO.getPostUuid();

    Post post =
        postRepository
            .findById(postUuid)
            .orElseThrow(() -> new NotFoundException("Post doesnt exist"));
    User user =
        userRepository
            .findById(userUuid)
            .orElseThrow(() -> new NotFoundException("User doesnt exist"));
    PostInteraction interaction =
        postInteractionRepository
            .findByUserIdAndPostUuid(userUuid, postUuid)
            .orElse(
                PostInteraction.builder()
                    .user(user)
                    .post(post)
                    .isLiked(false)
                    .isParticipating(false)
                    .build());

    if (info.changeLike()) {
      boolean newLikeState = !interaction.isLiked();
      interaction.setLiked(newLikeState);
      if (newLikeState) {
        postRepository.incrementLikes(postUuid);
      } else {
        postRepository.decrementLikes(postUuid);
      }
    }

    if (info.changeParticipating()) {
      boolean newParticipatingState = !interaction.isParticipating();
      interaction.setParticipating(newParticipatingState);
      if (newParticipatingState) {
        postRepository.incrementParticipating(postUuid);
      } else {
        postRepository.decrementParticipating(postUuid);
      }
    }
    postInteractionRepository.save(interaction);
  }

  @Transactional
  public PostCommentDTO commentPost(@Valid PostCommentDTO postCommentDTO) {
    Post post =
        postRepository
            .findById(postCommentDTO.getPostUuid())
            .orElseThrow(
                () -> new NotFoundException("The post you want to add comment to doesn't exist."));
    User author =
        userRepository
            .findById(postCommentDTO.getAuthor().getId())
            .orElseThrow(() -> new NotFoundException("User doesn't exist."));
    PostComment parent = null;
    if (postCommentDTO.getParentUuid() != null) {
      parent =
          postCommentRepository
              .findById(postCommentDTO.getParentUuid())
              .orElseThrow(() -> new NotFoundException("Parent comment doesn't exist"));
    }
    PostComment newComment =
        PostComment.builder()
            .content(postCommentDTO.getContent())
            .author(author)
            .post(post)
            .parentComment(parent)
            .likesCount(0)
            .replies(new ArrayList<>())
            .build();
    postCommentRepository.save(newComment);
    return PostCommentDTO.fromEntity(newComment);
  }

  public InteractionChangeInfo getInteractionState(UUID userUuid, UUID postUUID) {
    PostInteraction interaction =
        postInteractionRepository
            .findByUserIdAndPostUuid(userUuid, postUUID)
            .orElseThrow(() -> new NotFoundException("The post or user does not exist."));
    return new InteractionChangeInfo(interaction.isLiked(), interaction.isParticipating());
  }
}
