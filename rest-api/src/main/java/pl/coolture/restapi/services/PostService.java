package pl.coolture.restapi.services;


import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pl.coolture.restapi.dtos.Post.CreateUpdatePostDTO;
import pl.coolture.restapi.dtos.Post.GetPostDTO;
import pl.coolture.restapi.exceptionHandlers.exceptionTypes.NotFoundException;
import pl.coolture.restapi.models.Location;
import pl.coolture.restapi.models.Post;
import pl.coolture.restapi.user.domain.User;
import pl.coolture.restapi.repositories.LocationRepository;
import pl.coolture.restapi.repositories.PostRepository;
import pl.coolture.restapi.user.domain.UserRepository;

@Service
@RequiredArgsConstructor
public class PostService {
  private final PostRepository postRepository;
  private final UserRepository userRepository;
  private final LocationRepository locationRepository;
  private final ImageService imageService;

  public GetPostDTO createPost(@Valid CreateUpdatePostDTO dto, List<MultipartFile> images) {
    User author =
        userRepository
            .findById(dto.getAuthorUuid())
            .orElseThrow(() -> new NotFoundException("Author doesn't exist."));

    List<UUID> savedPhotosUuids = imageService.saveImages(images, "posts");

    Post post =
        Post.builder()
            .title(dto.getTitle())
            .author(author)
            .dateOfEvent(dto.getDateOfEvent())
            .description(dto.getDescription())
            .photosUUID(savedPhotosUuids)
            .likesCount(0)
            .participatingCount(0)
            .build();
    postRepository.save(post);
    return GetPostDTO.fromEntity(post);
  }

  public List<GetPostDTO> getPosts(int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    Page<Post> postsPage = postRepository.findAll(pageable);
    return postsPage.getContent().stream().map(GetPostDTO::fromEntity).toList();
  }

  public GetPostDTO getPostById(UUID id) {
    return GetPostDTO.fromEntity(
        postRepository.findById(id).orElseThrow(() -> new NotFoundException("Post not found")));
  }
}
