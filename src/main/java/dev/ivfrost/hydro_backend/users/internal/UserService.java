package dev.ivfrost.hydro_backend.users.internal;

import com.auth0.jwt.interfaces.Claim;
import dev.ivfrost.hydro_backend.devices.DeviceLinkRequest;
import dev.ivfrost.hydro_backend.devices.DeviceResponse;
import dev.ivfrost.hydro_backend.devices.DeviceUnlinkRequest;
import dev.ivfrost.hydro_backend.devices.DeviceUpdateRequest;
import dev.ivfrost.hydro_backend.tokens.JWTUtil;
import dev.ivfrost.hydro_backend.tokens.MqttTokenPayload;
import dev.ivfrost.hydro_backend.tokens.TokenPayload;
import dev.ivfrost.hydro_backend.tokens.TokenResponse;
import dev.ivfrost.hydro_backend.tokens.UserTokenProvider;
import dev.ivfrost.hydro_backend.devices.DeviceTopicProvider;
import dev.ivfrost.hydro_backend.users.AuthResponse;
import dev.ivfrost.hydro_backend.users.EmailTakenException;
import dev.ivfrost.hydro_backend.users.UserAuthRequest;
import dev.ivfrost.hydro_backend.devices.UserDeviceProvider;
import dev.ivfrost.hydro_backend.users.UserDisabledException;
import dev.ivfrost.hydro_backend.users.UserMqttResponse;
import dev.ivfrost.hydro_backend.users.UserRecoveryRequest;
import dev.ivfrost.hydro_backend.users.UserRegisterRequest;
import dev.ivfrost.hydro_backend.users.UserResponse;
import dev.ivfrost.hydro_backend.users.UserUpdateRequest;
import dev.ivfrost.hydro_backend.users.UsernameTakenException;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
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


  /**
   * Authenticates a user by email and password.
   *
   * @param req the user authentication request DTO
   * @return {@link AuthResponse} containing the authenticated user and access/refresh tokens
   * @throws AuthenticationCredentialsNotFoundException if the user is not found
   * @throws UserDisabledException                          if the user is disabled
   * @throws BadCredentialsException                    if the password is incorrect
   */
  AuthResponse authenticateUser(UserAuthRequest req) {
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
    List<TokenResponse> tokens = userTokenProvider.generateAccessAndRefreshTokens(new TokenPayload(
        user.getUsername(),
        user.getEmail(),
        user.getRoles().stream().map(role -> role.getRole().toString()).toList(),
        user.getId()
    ));
    return new AuthResponse(convertUserToResponse(user), tokens);
  }

  /**
   * Registers a new user with specified roles (admin only).
   *
   * @param req the user registration request DTO
   * @param roles the roles to assign to the user (defaults to USER if null)
   * @return {@link AuthResponse} containing the registered user and recovery tokens
   * @throws UsernameTakenException if the username is already taken
   */
  @Transactional
  AuthResponse addUser(UserRegisterRequest req, List<UserRole.Role> roles) {
    if (isUserAuthenticated()) {
      throw new IllegalStateException("Cannot register new user while authenticated.");
    }
    if (userRepository.findByUsername(req.username()).isPresent()) {
      throw new UsernameTakenException(req.username());
    }
    if (userRepository.findByEmail(req.email()).isPresent()) {
      throw new EmailTakenException(req.email());
    }
    User user = convertRequestToUser(req);
    // MapsId guarantees that the userId in UserRole is populated with the correct value
    user.getRoles().addAll(
        roles.stream()
            .map(role -> new UserRole(user, role))
            .toList()
    );
    User savedUser = userRepository.save(user);

    List<TokenResponse> recoveryTokens = userTokenProvider.generateRecoveryCodes(savedUser.getId());
    List<TokenResponse> accessRefreshTokens = userTokenProvider.generateAccessAndRefreshTokens(new TokenPayload(
        savedUser.getUsername(),
        savedUser.getEmail(),
        savedUser.getRoles().stream().map(role -> role.getRole().toString()).toList(),
        savedUser.getId()
    ));
    List<TokenResponse> allTokens = Stream.concat(recoveryTokens.stream(), accessRefreshTokens.stream()).toList();
    return new AuthResponse(convertUserToResponse(savedUser), allTokens);
  }

  /**
   * Registers a new user with default roles (self-registration).
   *
   * <p>- First user is assigned ADMIN and USER roles.
   *
   * @param req the user registration request DTO
   * @return {@link AuthResponse} containing the registered user and recovery tokens
   * @throws UsernameTakenException if the username is already taken
   */
  @Transactional
  AuthResponse addUser(UserRegisterRequest req) {
    boolean isFirstUser = userRepository.count() == 0;
    List<UserRole.Role> roles = isFirstUser
        ? List.of(UserRole.Role.ADMIN, UserRole.Role.USER)
        : List.of(UserRole.Role.USER);
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
    User user = userRepository.findById(userId).orElseThrow(
        () -> new AuthenticationCredentialsNotFoundException(
            "User with ID " + userId + " not found.")
    );
    if (!user.isEnabled()) {
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
    boolean isChangingPassword = req.password() != null && !req.password().isBlank();
    boolean isChangingEmail = req.email() != null && !req.email().isBlank() && !req.email().trim().toLowerCase().equals(user.getEmail());
    boolean isChangingUsername = req.username() != null && !req.username().isBlank() && !req.username().equals(user.getUsername());

    if (isChangingPassword || isChangingEmail) {
      if (req.currentPassword() == null || req.currentPassword().isBlank()) {
        throw new IllegalArgumentException("Current password must be provided to update credentials.");
      }

      if (!passwordEncoder.matches(req.currentPassword(), user.getPassword())) {
        log.debug("Password mismatch for user with email: {}", user.getEmail());
        throw new BadCredentialsException("Invalid credentials");
      }

      // If changing password, encode and update it
      if (isChangingPassword) {
        user.setPassword(passwordEncoder.encode(req.password()));
      }

      // If changing email, check if the new email is already in use and update it
      if (isChangingEmail) {
        String cleanEmail = req.email().trim().toLowerCase();
        boolean emailExists = userRepository.existsByEmail(cleanEmail);
        if (emailExists) {
          throw new IllegalArgumentException("Email address is already in use by another account.");
        }
        user.setEmail(cleanEmail);
      }
    }

    // If changing username, check if the new username is already in use and update it
    if (isChangingUsername) {
      boolean isUsernameTaken = userRepository.existsByUsername(req.username());
      if (isUsernameTaken && !req.username().equals(user.getUsername())) {
        throw new UsernameTakenException(req.username());
      }
      user.setUsername(req.username());
    }
    if (req.fullName() != null && !req.fullName().isBlank()) {
      user.setFullName(req.fullName());
    }
    if (req.phoneNumber() != null) {
      user.setPhoneNumber(req.phoneNumber());
    }
    if (req.address() != null) {
      user.setAddress(req.address());
    }

    // TODO: Reenable profile picture upload once blob storage is set up
    //    if (profilePicture != null && !profilePicture.isEmpty()) {
    //      String key = null;
    //      try {
    //        key = blobStorageService.save(profilePicture, userId);
    //      } catch (IOException e) {
    //        throw new RuntimeException(e);
    //      }
    //      user.setProfilePictureUrl(key);
    //    }

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
    Map<String, Claim> claims = userTokenProvider.validateTokenAndRetrieveClaims(refreshToken);
    Long tokenUserId = claims.get("userId").asLong();

    User user = userRepository.findById(tokenUserId)
        .orElseThrow(() -> new BadCredentialsException("User not found"));

    return userTokenProvider.generateAccessAndRefreshTokens(new TokenPayload(
        user.getUsername(),
        user.getEmail(),
        user.getRoles().stream().map(role -> role.getRole().toString()).toList(),
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
        null,
        topics
    )));
  }

  /**
   * Links a device to the currently authenticated user.
   */
  DeviceResponse linkDeviceToCurrentUser(DeviceLinkRequest req) {
    return deviceLinkProvider.linkDevice(req, getCurrentUserId());
  }

  /*
   * Unlink a device from the currently authenticated user.
   */
  void unlinkDeviceFromCurrentUser(DeviceUnlinkRequest req) {
    deviceLinkProvider.unlinkDevice(req, getCurrentUserId());
  }

  /*
   * Updates a device linked to the currently authenticated user.
   */
  DeviceResponse updateDeviceForCurrentUser(long deviceId, DeviceUpdateRequest req) {
    User user = getCurrentUser();
    boolean isAdmin = user.getRoles().contains(UserRole.Role.ADMIN);
    return userDeviceProvider.updateUserDevice(deviceId, req, user.getId(), isAdmin);
  }

  /*
   * Persists UI device display order for the currently authenticated user.
   */
  void persistDeviceOrderForCurrentUser(List<Long> deviceOrder) {
    userDeviceProvider.persistDeviceOrder(getCurrentUserId(), deviceOrder);
  }

  /*
   * Retrieves devices linked to the currently authenticated user.
   */
  List<DeviceResponse> getDevicesForCurrentUser() {
    return userDeviceProvider.getUserDevices(getCurrentUserId());
  }

  boolean validateUsernameEmail(String username, String email) {
    if (username != null && !username.isBlank()) {
      return userRepository.findByUsername(username).isEmpty();
    }
    if (email != null && !email.isBlank()) {
      return userRepository.findByEmail(email).isEmpty();
    }
    return false; // neither field provided — nothing to validate
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
        .map(role -> role.getRole().toString())
        .toList();

    return new UserResponse(
        user.getId(), user.getUsername(), user.getFullName(), user.getEmail(),
        user.getProfilePictureUrl(),
        user.getPhoneNumber(), user.getAddress(), user.getCreatedAt(), user.getUpdatedAt(),
        roleList, user.getSettings()
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
