package dev.ivfrost.hydro_backend.users.internal;

import com.auth0.jwt.interfaces.Claim;
import dev.ivfrost.hydro_backend.devices.DeviceLinkRequest;
import dev.ivfrost.hydro_backend.devices.DeviceResponse;
import dev.ivfrost.hydro_backend.tokens.JWTUtil;
import dev.ivfrost.hydro_backend.tokens.MqttTokenPayload;
import dev.ivfrost.hydro_backend.tokens.TokenPayload;
import dev.ivfrost.hydro_backend.tokens.TokenResponse;
import dev.ivfrost.hydro_backend.tokens.UserTokenProvider;
import dev.ivfrost.hydro_backend.users.BlobStorageService;
import dev.ivfrost.hydro_backend.devices.DeviceTopicProvider;
import dev.ivfrost.hydro_backend.users.UserAuthRequest;
import dev.ivfrost.hydro_backend.devices.UserDeviceProvider;
import dev.ivfrost.hydro_backend.users.UserDisabledException;
import dev.ivfrost.hydro_backend.users.UserMqttResponse;
import dev.ivfrost.hydro_backend.users.UserRecoveryRequest;
import dev.ivfrost.hydro_backend.users.UserRegisterRequest;
import dev.ivfrost.hydro_backend.users.UserRegisterResponse;
import dev.ivfrost.hydro_backend.users.UserResponse;
import dev.ivfrost.hydro_backend.users.UserUpdateLastOnlineEvent;
import dev.ivfrost.hydro_backend.users.UserUpdateRequest;
import dev.ivfrost.hydro_backend.users.UsernameTakenException;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@AllArgsConstructor()
@Service
public class UserService {

  private static final long ONLINE_THRESHOLD_MS = 300_000; // 5 minutes
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JWTUtil jwtUtil;
  private final DeviceTopicProvider deviceTopicProvider;
  private final UserTokenProvider userTokenProvider;
  private final RedisTemplate<Object, Object> redisTemplate;
  private final ApplicationEventPublisher events;
  private final UserDeviceProvider userDeviceProvider;
  private final dev.ivfrost.hydro_backend.devices.DeviceLinkProvider deviceLinkProvider;
  private final BlobStorageService blobStorageService;


  /**
   * Authenticates a user by email and password.
   *
   * @param req the user authentication request DTO
   * @return a list of {@link TokenResponse} containing access and refresh tokens
   * @throws AuthenticationCredentialsNotFoundException if the user is not found
   * @throws UserDisabledException                          if the user is disabled
   * @throws BadCredentialsException                    if the password is incorrect
   */
  List<TokenResponse> authenticateUser(UserAuthRequest req) {
    String email = req.email();
    String password = req.password();
    Optional<User> userOpt = userRepository.findByEmail(email);
    if (userOpt.isEmpty()) {
      log.debug("User not found with email: {}", email);
      throw new AuthenticationCredentialsNotFoundException(
          "Invalid credentials");
    }
    User user = userOpt.get();
    log.debug("Authenticating user with email: {}", email);
    if (!user.isEnabled()) {
      throw new UserDisabledException(email);
    }
    if (!passwordEncoder.matches(password, user.getPassword())) {
      log.debug("Password mismatch for user with email: {}", email);
      throw new BadCredentialsException("Invalid credentials");
    }
    return userTokenProvider.generateAccessAndRefreshTokens(new TokenPayload(
        user.getUsername(),
        user.getEmail(),
        user.getRoles().stream().map(Enum::name).toList(),
        user.getPreferredLanguage(),
        user.getId()
    ));
  }

  /**
   * Registers a new user with specified roles (admin only).
   *
   * @param req   the user registration request DTO
   * @param roles the roles to assign to the user (defaults to USER if null)
   * @return {@link UserRegisterResponse} containing recovery tokens
   * @throws UsernameTakenException if the username is already taken
   */
  @Transactional
  List<TokenResponse> addUser(UserRegisterRequest req, List<User.Role> roles) {
    if (isUserAuthenticated()) {
      throw new IllegalStateException("Cannot register new user while authenticated.");
    }
    if (userRepository.findByUsername(req.username()).isPresent()) {
      throw new UsernameTakenException(req.username());
    }
    if (userRepository.findByEmail(req.email()).isPresent()) {
      throw new UsernameTakenException(req.email());
    }
    User user = convertRequestToUser(req);
    user.setRoles(roles != null ? roles : List.of(User.Role.USER));
    User savedUser = userRepository.save(user);

    return userTokenProvider.generateRecoveryTokens(savedUser.getId());
  }

  /**
   * Registers a new user with default roles (self-registration).
   *
   * <p>- First user is assigned ADMIN and USER roles.
   *
   * @param req the user registration request DTO
   * @return {@link UserRegisterResponse} containing recovery tokens
   * @throws UsernameTakenException if the username is already taken
   */
  @Transactional
  List<TokenResponse> addUser(UserRegisterRequest req) {
    boolean isFirstUser = userRepository.count() == 0;
    List<User.Role> roles = isFirstUser
        ? List.of(User.Role.ADMIN, User.Role.USER)
        : List.of(User.Role.USER);
    return addUser(req, roles);
  }

  /**
   * Retrieves the authenticated user
   *
   * @return authenticated {@link User} entity
   * @throws AuthenticationCredentialsNotFoundException if the user is not found
   * @throws UserDisabledException                      if the user is disabled
   */
  private User getCurrentUser() {
    Long userId = getCurrentUserId();
    User user = userRepository.findById(userId).orElse(null);
    if (user == null) {
      throw new AuthenticationCredentialsNotFoundException(
          "User with ID " + userId + " not found.");
    } else if (!user.isEnabled()) {
      throw new UserDisabledException(userId);
    }
    return user;
  }

  /**
   * Retrieves the profile of the authenticated user.
   *
   * @return {@link UserResponse} containing user profile information
   */
  UserResponse getCurrentUserProfile() {
    return convertUserToResponse(getCurrentUser());
  }

  /**
   * Retrieves a user by ID.
   *
   * @param userId the user ID
   * @return {@link User} entity
   * @throws AuthenticationCredentialsNotFoundException if the user is not found
   */
  private User getUserById(Long userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
            "User with ID " + userId + " not found."));
  }

  /**
   * Retrieves a user profile by ID (admin only).
   *
   * @param userId the user ID
   * @return {@link UserResponse} containing user profile information
   */
  UserResponse getUserProfileById(Long userId) {
    return convertUserToResponse(getUserById(userId));
  }

  /**
   * Retrieves all user profiles (admin only, cached, paginated).
   *
   * @param pageable the pagination information
   * @return a page of {@link UserResponse} containing user profile information
   */
  @Cacheable(
      value = "allUsersCache",
      key = "'allUsers:' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort"
  )
  public Page<UserResponse> getAllUserProfiles(Pageable pageable) {
    return convertUsersToResponses(userRepository.findAll(pageable));
  }

  /**
   * Disables a user by ID.
   *
   * @param userId the user ID
   * @throws UserDisabledException                      if the user is already disabled
   * @throws AuthenticationCredentialsNotFoundException if the user is not found
   */
  void deleteUserById(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
            "User with ID " + userId + " not found."));
    if (!user.isEnabled()) {
      throw new UserDisabledException(userId);
    }
    user.setEnabled(false);
    userRepository.save(user);
  }

  /**
   * Disables the authenticated user.
   *
   */
  void deleteCurrentUser() {
    deleteUserById(getCurrentUserId());
  }

  /**
   * Resets the user's password using a recovery token.
   *
   * <p>Validates the recovery token and ensures it belongs to the provided email. If valid,
   * updates the user's password and invalidates the used token.
   *
   * @param req the user recovery request DTO containing email, recovery code, and new password
   * @throws AuthenticationCredentialsNotFoundException if the user is not found
   * @throws UserDisabledException                      if the user is disabled
   * @throws BadCredentialsException                    if the recovery code is invalid
   */
  @Transactional
  void resetPassword(UserRecoveryRequest req) {
    User user = userRepository.findByEmail(req.email()).orElseThrow(
        () -> new AuthenticationCredentialsNotFoundException(
            "User with email " + req.email() + " not found."));
    if (!user.isEnabled()) {
      throw new UserDisabledException(user.getId());
    }
    if (!userTokenProvider.isTokenValidForUserId(req.recoveryCode(), user.getId())) {
      throw new BadCredentialsException("Invalid recovery code.");
    }

    user.setPassword(passwordEncoder.encode(req.newPassword()));
    userRepository.save(user);
  }

  /**
   * Updates the authenticated user's account settings.
   *
   * @param req the user update request DTO containing fields to update
   * @return {@link UserResponse} containing updated user profile information
   * @throws IllegalStateException                      if no authenticated user is found
   * @throws AuthenticationCredentialsNotFoundException if the user is not found
   * @throws UserDisabledException                      if the user is disabled
   */
  @Transactional
  UserResponse updateCurrentUser(UserUpdateRequest req, MultipartFile profilePicture) {
    Long userId = getCurrentUser().getId();
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
            "User with ID " + userId + " not found."));
    if (!user.isEnabled()) {
      throw new UserDisabledException(userId);
    }
    if (req.username() != null && !req.username().isBlank()) {
      user.setUsername(req.username());
    }
    if (req.fullName() != null && !req.fullName().isBlank()) {
      user.setFullName(req.fullName());
    }
    if (req.email() != null && !req.email().isBlank()) {
      user.setEmail(req.email());
    }
    if (req.phoneNumber() != null) {
      user.setPhoneNumber(req.phoneNumber());
    }
    if (req.address() != null) {
      user.setAddress(req.address());
    }
    if (profilePicture != null && !profilePicture.isEmpty()) {
      String key = null;
      try {
        key = blobStorageService.save(profilePicture, userId);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      user.setProfilePictureUrl(key);
    }

    if (req.preferredLanguage() != null && !req.preferredLanguage().isBlank()) {
      user.setPreferredLanguage(req.preferredLanguage());
    }
    if (req.settings() != null) {
      user.setSettings(req.settings());
    }
    userRepository.save(user);
    return convertUserToResponse(user);
  }

  /**
   * Refreshes access and refresh tokens using a valid refresh token.
   *
   * @param refreshToken the refresh token to validate and use for generating new tokens
   * @return a list of {@link TokenResponse} containing new access and refresh tokens
   * @throws BadCredentialsException if the refresh token does not belong to the authenticated user
   */
  List<TokenResponse> refreshTokens(String refreshToken) {
    User user = getCurrentUser();

    Map<String, Claim> claims = userTokenProvider.validateTokenAndRetrieveClaims(
        refreshToken);
    Long tokenUserId = claims.get("userId").asLong();

    if (!user.getId().equals(tokenUserId)) {
      throw new BadCredentialsException("Refresh token does not belong to authenticated user");
    }

    return userTokenProvider.generateAccessAndRefreshTokens(new TokenPayload(
        user.getUsername(),
        user.getEmail(),
        user.getRoles().stream().map(Enum::name).toList(),
        user.getPreferredLanguage(),
        user.getId()
    ));
  }

  /**
   * Get short-lived RS256 signed JWT token for MQTT authentication.
   *
   * @return the MQTT authentication response containing the JWT token
   * @throws AuthenticationCredentialsNotFoundException if the user is not found
   */
  UserMqttResponse getMqttAuthToken() throws AuthenticationCredentialsNotFoundException {
    User user = getCurrentUser();
    List<String> topics = deviceTopicProvider.getTopicsForUser(user.getId());
    log.debug("Retrieved {} topics for user {}: {}", topics.size(), user.getId(), topics);
    return new UserMqttResponse(user.getId(), jwtUtil.generateMqttToken(new MqttTokenPayload(
        user.getId(),
        topics
    )));
  }

  /**
   * Links a device to the currently authenticated user.
   */
  void linkDeviceToCurrentUser(DeviceLinkRequest req) {
    deviceLinkProvider.linkDevice(req, getCurrentUserId());
  }

  /*
   * Unlink a device from the currently authenticated user.
   */
  void unlinkDeviceFromCurrentUser(DeviceLinkRequest req) {
    deviceLinkProvider.unlinkDevice(req, getCurrentUserId());
  }

  /*
   * Retrieves devices linked to the currently authenticated user.
   */
  List<DeviceResponse> getDevicesForCurrentUser() {
    return userDeviceProvider.getUserDevices(getCurrentUserId());
  }

  boolean validateUsernameEmail(String username, String email) {
    if (username != null && !username.isBlank() && userRepository.findByUsername(username)
        .isEmpty()) {
      return true;
    }
    if (email != null && !email.isBlank() && userRepository.findByEmail(email).isEmpty()) {
      return true;
    }
    return false;
  }

  /*====== REDIS STATE ======*/

  /**
   * Updates the currently authenticated user's last online value.
   *
   * @param username the username of the user to update the last online timestamp for
   */
  public void updateLastOnline(String username) {
    redisTemplate.opsForValue()
        .set("user:lastOnline:" + username, String.valueOf(System.currentTimeMillis()));
  }

  @ApplicationModuleListener
  void on(UserUpdateLastOnlineEvent e) {
    updateLastOnline(e.username());
  }

  /**
   * Retrieves the currently authenticated user's last online value.
   *
   * @param username the username of the user to retrieve the last online timestamp for
   * @return the last online timestamp in milliseconds, or null if not found
   */
  public Long getLastOnline(String username) {
    String lastOnlineStr = (String) redisTemplate.opsForValue().get("user:lastOnline:" + username);

    if (lastOnlineStr != null) {
      return Long.parseLong(lastOnlineStr);
    }
    return null;
  }

  public boolean isOnline(String username) {
    Long lastOnline = getLastOnline(username);
    if (lastOnline == null) {
      return false;
    }
    long currentTime = System.currentTimeMillis();
    return (currentTime - lastOnline) <= ONLINE_THRESHOLD_MS;
  }

  /*====== HELPERS ======*/

  /**
   * Checks if a user is authenticated in the security context.
   *
   * @return true if a user is authenticated, false otherwise
   */
  public boolean isUserAuthenticated() {
    SecurityContext context = SecurityContextHolder.getContext();
    var auth = context.getAuthentication();
    return auth != null && auth.isAuthenticated()
        && !(auth instanceof AnonymousAuthenticationToken);
  }

  public Long getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      throw new AuthenticationCredentialsNotFoundException("No authenticated user found.");
    }
    return Long.parseLong(authentication.getName());
  }

  /**
   * Converts a UserRegisterRequest DTO to a User entity.
   *
   * @param req the user registration request DTO
   * @return the user entity
   */
  public User convertRequestToUser(UserRegisterRequest req) {
    String encodedPassword = passwordEncoder.encode(req.password());
    User user = new User();
    user.setUsername(req.username());
    user.setPassword(encodedPassword);
    user.setEmail(req.email());
    user.setFullName(req.fullName());
    user.setPreferredLanguage(req.preferredLanguage());
    return user;
  }

  /**
   * Converts a User entity to a UserResponse DTO.
   *
   * @param user the user entity
   * @return the user response DTO
   */
  public UserResponse convertUserToResponse(User user) {
    if (user == null) {
      return null;
    }
    List<String> roleList = user.getRoles().stream()
        .map(Enum::name)
        .toList();

    return new UserResponse(
        user.getId(), user.getUsername(), user.getFullName(), user.getEmail(),
        user.getProfilePictureUrl(),
        user.getPhoneNumber(), user.getAddress(), user.getCreatedAt(), user.getUpdatedAt(),
        roleList, user.getPreferredLanguage(), user.getSettings()
    );
  }

  /**
   * Converts a list of User entities to a list of UserResponse DTOs.
   *
   * @param users the list of user entities
   * @return the list of user response DTOs
   */
  public Page<UserResponse> convertUsersToResponses(Page<User> users) {
    return users.map(this::convertUserToResponse);
  }
}
