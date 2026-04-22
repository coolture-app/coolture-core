package pl.coolture.restapi.user.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.coolture.restapi.user.domain.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {
  private final UserRepository userRepository;
//
//  private boolean isApplicationRole(String authority) {
//    return authority.startsWith("ROLE_") && !authority.equals("ROLE_OAUTH2_USER");
//  }
//
//  private String extractUserId(OidcUser principal) {
//    return principal.getSubject();
//  }
//
//  public GetCurrentUserDTO getCurrentUser(OidcUser principal) {
//    List<String> roles =
//        principal.getAuthorities().stream()
//            .map(GrantedAuthority::getAuthority)
//            .filter(this::isApplicationRole)
//            .collect(Collectors.toList());
//
//    String userId = extractUserId(principal);
//
//    return GetCurrentUserDTO.builder()
//        .userId(userId)
//        .username(principal.getPreferredUsername())
//        .email(principal.getEmail())
//        .fullName(principal.getFullName())
//        .roles(roles)
//        .build();
//  }
//
//  @Transactional(readOnly = true)
//  public CreateUpdateUserDTO getUserById(UUID id) {
//    User user =
//        userRepository
//            .findById(id)
//            .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
//    return CreateUpdateUserDTO.fromUser(user);
//  }
//
//  @Transactional(readOnly = true)
//  public CreateUpdateUserDTO getUserByUsername(String username) {
//    User user =
//        userRepository
//            .findByUsername(username)
//            .orElseThrow(() -> new NotFoundException("User not found with username: " + username));
//    return CreateUpdateUserDTO.fromUser(user);
//  }
//
//  @Transactional
//  public CreateUpdateUserDTO createUser(CreateUpdateUserDTO createUpdateUserDTO) {
//    if (userRepository.existsByUsername(createUpdateUserDTO.getUsername())) {
//      throw new AlreadyExistsException("User with given username already exist");
//    }
//    if (userRepository.existsByEmail(createUpdateUserDTO.getEmail())) {
//      throw new AlreadyExistsException("User with given email already exists");
//    }
//    User user = createUpdateUserDTO.toUser();
//    User savedUser = userRepository.save(user);
//    return CreateUpdateUserDTO.fromUser(savedUser);
//  }
//
//  @Transactional
//  public CreateUpdateUserDTO updateUser(UUID id, CreateUpdateUserDTO createUpdateUserDTO) {
//    User user =
//        userRepository
//            .findById(id)
//            .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
//
//    if (createUpdateUserDTO.getUsername() != null
//        && !user.getUsername().equals(createUpdateUserDTO.getUsername())
//        && userRepository.existsByUsername(createUpdateUserDTO.getUsername())) {
//      throw new AlreadyExistsException("User with given username already exist");
//    }
//    if (createUpdateUserDTO.getEmail() != null
//        && !user.getEmail().equals(createUpdateUserDTO.getEmail())
//        && userRepository.existsByEmail(createUpdateUserDTO.getEmail())) {
//      throw new AlreadyExistsException("User with given email already exists");
//    }
//    if (createUpdateUserDTO.getFirstName() != null) {
//      user.setFirstName(createUpdateUserDTO.getFirstName());
//    }
//    if (createUpdateUserDTO.getLastName() != null) {
//      user.setLastName(createUpdateUserDTO.getLastName());
//    }
//    if (createUpdateUserDTO.getBioDescription() != null) {
//      user.setBioDescription(createUpdateUserDTO.getBioDescription());
//    }
//
//    return CreateUpdateUserDTO.fromUser(user);
//  }
//
//  @Transactional
//  public void deleteUser(UUID id) {
//    User user =
//        userRepository
//            .findById(id)
//            .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
//    userRepository.delete(user);
//  }
}
