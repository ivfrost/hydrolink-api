package dev.ivfrost.hydro_backend.users.internal;

import dev.ivfrost.hydro_backend.devices.AdminDeviceLinkRequest;
import dev.ivfrost.hydro_backend.devices.DeviceLinkProvider;
import dev.ivfrost.hydro_backend.devices.DeviceLinkRequest;
import dev.ivfrost.hydro_backend.devices.DeviceResponse;
import dev.ivfrost.hydro_backend.devices.DeviceUnlinkRequest;
import dev.ivfrost.hydro_backend.devices.DeviceUpdateRequest;
import dev.ivfrost.hydro_backend.devices.UserDeviceProvider;
import dev.ivfrost.hydro_backend.users.AuthenticatedUser;
import dev.ivfrost.hydro_backend.users.CognitoUserSyncService;
import dev.ivfrost.hydro_backend.users.EmailNotVerifiedException;
import dev.ivfrost.hydro_backend.users.ReauthenticationRequiredException;
import dev.ivfrost.hydro_backend.users.UserDisabledException;
import dev.ivfrost.hydro_backend.users.UserMapper;
import dev.ivfrost.hydro_backend.users.UserResponse;
import dev.ivfrost.hydro_backend.users.UserUpdateRequest;
import dev.ivfrost.hydro_backend.users.UsernameTakenException;
import dev.ivfrost.hydro_backend.users.UserResolutionService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@AllArgsConstructor()
@Service
public class UserService {

  private final UserRepository userRepository;
  private final UserDeviceProvider userDeviceProvider;
  private final DeviceLinkProvider deviceLinkProvider;
  private final UserMapper userMapper;
  private final CognitoUserSyncService cognitoUserSyncService;
  private final dev.ivfrost.hydro_backend.storage.OtaUpdateService otaUpdateService;

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
    return toSignedUserResponse(getCurrentUser());
  }

  /**
   * Retrieves a user by ID.
   *
   * @param userId the user ID
   * @return {@link User} entity
   * @throws AuthenticationCredentialsNotFoundException if the user is not found
   */
  private User getUserById(UUID userId) {
    return requireUserById(userId);
  }

  /**
   * Retrieves a user profile by ID (admin only).
   *
   * @param userId the user ID
   * @return {@link UserResponse} containing user profile information
   */
  UserResponse getUserProfileById(UUID userId) {
    return toSignedUserResponse(getUserById(userId));
  }

  /**
   * Retrieves all user profiles (admin only, cached, paginated, and sorted descending by creation date).
   *
   * @param pageable the pagination and sorting information
   * @return a page of {@link UserResponse} containing user profile information
   */
  public Page<UserResponse> getAllUserProfiles(Pageable pageable) {
    return getAllUserProfilesRaw(pageable).map(this::signUserImage);
  }

  @Cacheable(
      value = "allUsersCache",
      key = "'allUsers:' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort"
  )
  Page<UserResponse> getAllUserProfilesRaw(Pageable pageable) {
    return userRepository.findAll(pageable).map(userMapper::userToUserResponse);
  }

  /**
   * Disables a user by ID.
   *
   * @param userId the user ID
   * @throws UserDisabledException                      if the user is already disabled
   * @throws AuthenticationCredentialsNotFoundException if the user is not found
   */
  void disableUserById(UUID userId) {
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
   * Updates the authenticated user's account settings.
   *
   * @param req the user update request DTO containing fields to update
   * @return {@link UserResponse} containing updated user profile information
   * @throws IllegalStateException                      if no authenticated user is found, or if
   *                                                      auth_time is missing from the provided JWT
   * @throws AuthenticationCredentialsNotFoundException if the user is not found
   * @throws UserDisabledException                      if the user is disabled
   * @throws ReauthenticationRequiredException          if the user's last authentication is too old
   *                                                      to change email
   */
  @Transactional
  UserResponse updateCurrentUser(UserUpdateRequest req, Jwt jwt) {
    User user = getCurrentUser();
    if (!user.isEnabled()) {
      throw new UserDisabledException("Account is disabled.");
    }
    if (!user.isEmailVerified()) {
      throw new EmailNotVerifiedException("Verify your email before updating your profile");
    }

    // Normalize inputs
    String cleanEmail = StringUtils.hasText(req.email()) ? req.email().trim().toLowerCase() : null;
    boolean isChangingEmail = cleanEmail != null && !cleanEmail.equals(user.getEmail());
    boolean isChangingUsername = StringUtils.hasText(req.username()) && !req.username().equals(user.getUsername());

    if (isChangingEmail) {
      Long authTimeEpoch = jwt.getClaim("auth_time");
      if (Objects.isNull(authTimeEpoch)) {
        throw new IllegalStateException("auth_time is missing from the provided JWT");
      }
      Instant authTime = Instant.ofEpochSecond(authTimeEpoch);
      if (Instant.now().minus(10, ChronoUnit.MINUTES).isAfter(authTime)) {
        throw new ReauthenticationRequiredException();
      }

      String sub = jwt.getSubject();
      if (Objects.isNull(sub)) {
        throw new IllegalStateException("sub is missing from the provided JWT");
      }

      if (isEmailClaimedByAnotherUser(cleanEmail, user.getId())) {
        throw new IllegalArgumentException("Email address is already in use by another account.");
      }

      cognitoUserSyncService.syncUserEmail(sub, cleanEmail);
      user.setPendingEmail(cleanEmail);
      user.setPendingEmailSetAt(Instant.now());
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
    return toSignedUserResponse(user);
  }

  /**
   * Links a device to the currently authenticated user.
   */
  DeviceResponse linkDeviceToCurrentUser(DeviceLinkRequest req, String userSub, String identityId, UUID userId) {
    return deviceLinkProvider.linkDevice(req, userSub, identityId, userId);
  }

  /**
   * Admin link. The admin is role-gated and has no token for the target user, so the
   * identity id is supplied by the caller while the target's sub comes from the row.
   */
  DeviceResponse adminLinkDevice(AdminDeviceLinkRequest req, UUID targetUserId) {
    User target = requireUserById(targetUserId);
    if (target.getSub() == null || target.getSub().isBlank()) {
      throw new IllegalStateException(
          "Cannot link a device to a user who has not signed in yet: the user has no Cognito sub.");
    }
    return deviceLinkProvider.linkDevice(
        new DeviceLinkRequest(req.secret()), target.getSub(), req.cognitoId(), targetUserId);
  }

  /*
   * Unlink a device from the currently authenticated user.
   */
  void unlinkDeviceFromCurrentUser(DeviceUnlinkRequest req, String identityId, UUID userId) {
    deviceLinkProvider.unlinkDevice(req, identityId, userId);
  }

  /**
   * Updates a device linked to the currently authenticated user.
   *
   * @param deviceKey the unique key of the device to update
   * @param req the device update request containing the new device information
   * @return {@link DeviceResponse} containing the updated device information
   */
  DeviceResponse updateDeviceForCurrentUser(String deviceKey, DeviceUpdateRequest req) {
    User user = getCurrentUser();
    return userDeviceProvider.updateUserDevice(deviceKey, req, user.getId());
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
    return false; // neither field provided, nothing to validate
  }

  /*====== HELPERS ======*/

  /**
   * True when the address is already taken by a different user, either as a
   * committed email or as a still-valid pending change. An expired pending
   * email no longer claims the address, and is cleared here so it stops
   * occupying the unique constraint and blocking the new claim.
   */
  private boolean isEmailClaimedByAnotherUser(String email, UUID currentUserId) {
    Optional<User> byEmail = userRepository.findByEmail(email);
    if (byEmail.isPresent() && !byEmail.get().getId().equals(currentUserId)) {
      return true;
    }
    Optional<User> byPending = userRepository.findByPendingEmail(email);
    if (byPending.isEmpty() || byPending.get().getId().equals(currentUserId)) {
      return false;
    }
    User holder = byPending.get();
    if (UserResolutionService.pendingEmailExpired(holder.getPendingEmailSetAt())) {
      log.info("Clearing expired pending email {} held by user {}", email, holder.getId());
      holder.setPendingEmail(null);
      holder.setPendingEmailSetAt(null);
      userRepository.save(holder);
      return false;
    }
    return true;
  }

  /**
   * Retrieves a user by ID, throwing an exception if not found.
   *
   * @param userId the user ID
   * @return {@link User} entity
   * @throws AuthenticationCredentialsNotFoundException if the user is not found
   */
  private User requireUserById(UUID userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
            "User with ID " + userId + " not found."));
  }

  /**
   * Retrieves the ID of the currently authenticated user.
   *
   * @return the user ID
   * @throws AuthenticationCredentialsNotFoundException if no authenticated user is found
   */
  public UUID getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      throw new AuthenticationCredentialsNotFoundException("No authenticated user found.");
    }
    Object principal = authentication.getPrincipal();
    if (principal instanceof AuthenticatedUser authenticatedUser) {
      return authenticatedUser.id();
    }
    throw new AuthenticationCredentialsNotFoundException("No authenticated user found.");
  }

  private UserResponse toSignedUserResponse(User user) {
    return signUserImage(userMapper.userToUserResponse(user));
  }

  /**
   * Signs the stored image key into a fresh URL for the response. Rows holding an old
   * full URL are left as-is.
   */
  private UserResponse signUserImage(UserResponse response) {
    String imageUrl = response.imageUrl();
    if (imageUrl == null || imageUrl.isBlank() || imageUrl.startsWith("http")) {
      return response;
    }
    try {
      return response.toBuilder()
          .imageUrl(otaUpdateService.generatePresignedUrl(imageUrl))
          .build();
    } catch (Exception e) {
      log.warn("Could not sign image key {} for user {}; returning key as-is",
          imageUrl, response.id(), e);
      return response;
    }
  }
}
