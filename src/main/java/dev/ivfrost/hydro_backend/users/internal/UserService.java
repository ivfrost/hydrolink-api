package dev.ivfrost.hydro_backend.users.internal;

import com.auth0.jwt.interfaces.Claim;
import dev.ivfrost.hydro_backend.devices.DeviceLinkProvider;
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
import dev.ivfrost.hydro_backend.users.UserMapper;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@AllArgsConstructor()
@Service
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JWTUtil jwtUtil;
  private final DeviceTopicProvider deviceTopicProvider;
  private final UserTokenProvider userTokenProvider;
  private final UserDeviceProvider userDeviceProvider;
  private final DeviceLinkProvider deviceLinkProvider;
  private final UserMapper userMapper;

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
    User user = requireUserByEmail(email);
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
        userMapper.mapRoles(user.getRoles()),
        user.getId()
    ));
    return new AuthResponse(userMapper.userToUserResponse(user), tokens);
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
    User user = userMapper.userRegisterRequestToUser(req);
    user.setPassword(passwordEncoder.encode(req.password()));
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
        userMapper.mapRoles(user.getRoles()),
        savedUser.getId()
    ));
    List<TokenResponse> allTokens = Stream.concat(recoveryTokens.stream(), accessRefreshTokens.stream()).toList();
    return new AuthResponse(userMapper.userToUserResponse(savedUser), allTokens);
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
    User user = requireUserById(getCurrentUserId());
    if (!user.isEnabled()) {
      throw new UserDisabledException(user.getId());
    }
    return user;
  }

  /**
   * Retrieves the profile of the authenticated user.
   *
   * @return {@link UserResponse} containing user profile information
   */
  UserResponse getCurrentUserProfile() {
    return userMapper.userToUserResponse(getCurrentUser());
  }

  /**
   * Retrieves a user by ID.
   *
   * @param userId the user ID
   * @return {@link User} entity
   * @throws AuthenticationCredentialsNotFoundException if the user is not found
   */
  private User getUserById(Long userId) {
    return requireUserById(userId);
  }

  /**
   * Retrieves a user profile by ID (admin only).
   *
   * @param userId the user ID
   * @return {@link UserResponse} containing user profile information
   */
  UserResponse getUserProfileById(Long userId) {
    return userMapper.userToUserResponse(getUserById(userId));
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
    return userRepository.findAll(pageable).map(userMapper::userToUserResponse);
  }

  /**
   * Disables a user by ID.
   *
   * @param userId the user ID
   * @throws UserDisabledException                      if the user is already disabled
   * @throws AuthenticationCredentialsNotFoundException if the user is not found
   */
  void disableUserById(Long userId) {
    User user = requireUserById(userId);
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
  void disableCurrentUser() {
    disableUserById(getCurrentUserId());
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
    User user = requireUserByEmail(req.email());
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
  UserResponse updateCurrentUser(UserUpdateRequest req) {
    User user = getCurrentUser();

    // Normalize inputs
    String cleanEmail = StringUtils.hasText(req.email()) ? req.email().trim().toLowerCase() : null;
    boolean isCurrentPasswordProvided = StringUtils.hasText(req.currentPassword());
    boolean isChangingPassword = StringUtils.hasText(req.password());
    boolean isChangingEmail = cleanEmail != null && !cleanEmail.equals(user.getEmail());
    boolean isChangingUsername = StringUtils.hasText(req.username()) && !req.username().equals(user.getUsername());

    // Validate email and password changes
    if (isChangingPassword || isChangingEmail) {
      if (!isCurrentPasswordProvided) {
        throw new IllegalArgumentException("Current password must be provided to update credentials.");
      }

      if (!passwordEncoder.matches(req.currentPassword(), user.getPassword())) {
        log.debug("Password mismatch for user with email: {}", user.getEmail());
        throw new BadCredentialsException("Invalid credentials");
      }

      if (isChangingPassword) {
        user.setPassword(passwordEncoder.encode(req.password()));
      }

      if (isChangingEmail) {
        if (userRepository.existsByEmail(cleanEmail)) {
          throw new IllegalArgumentException("Email address is already in use by another account.");
        }
        user.setEmail(cleanEmail);
      }
    }

    // If changing username, ensure the new username is not already in use and update it
    if (isChangingUsername) {
      boolean isUsernameTaken = userRepository.existsByUsername(req.username());
      if (isUsernameTaken) {
        throw new UsernameTakenException(req.username());
      }
      user.setUsername(req.username());
    }

    // Update remaining optional profile fields
    userMapper.updateUserFromRequest(req, user);

    // Hibernate dirty checking handles updates
    return userMapper.userToUserResponse(user);
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
        userMapper.mapRoles(user.getRoles()),
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
    boolean isAdmin = userMapper.mapRoles(user.getRoles()).contains(UserRole.Role.ADMIN.toString());
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

  /*
   * Validates that the provided username and/or email are not already taken by another user.
   */
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

  /**
   * Retrieves a user by ID, throwing an exception if not found.
   *
   * @param userId the user ID
   * @return {@link User} entity
   * @throws AuthenticationCredentialsNotFoundException if the user is not found
   */
  private User requireUserById(Long userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
            "User with ID " + userId + " not found."));
  }

  /**
   * Retrieves a user by email, throwing an exception if not found.
   *
   * @param email the user email
   * @return {@link User} entity
   * @throws AuthenticationCredentialsNotFoundException if the user is not found
   */
  private User requireUserByEmail(String email) {
    return userRepository.findByEmail(email)
        .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
            "User with email " + email + " not found."));
  }

  /**
   * Retrieves the ID of the currently authenticated user.
   *
   * @return the user ID
   * @throws AuthenticationCredentialsNotFoundException if no authenticated user is found
   */
  public Long getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      throw new AuthenticationCredentialsNotFoundException("No authenticated user found.");
    }
    return Long.parseLong(authentication.getName());
  }
}
