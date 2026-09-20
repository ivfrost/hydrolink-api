package dev.ivfrost.hydro_backend.users.internal;

import dev.ivfrost.hydro_backend.common.ApiResponse;
import dev.ivfrost.hydro_backend.devices.AdminDeviceLinkRequest;
import dev.ivfrost.hydro_backend.devices.DeviceLinkRequest;
import dev.ivfrost.hydro_backend.devices.DeviceResponse;
import dev.ivfrost.hydro_backend.devices.DeviceUnlinkRequest;
import dev.ivfrost.hydro_backend.devices.DeviceUpdateRequest;
import dev.ivfrost.hydro_backend.users.AuthenticatedUser;
import dev.ivfrost.hydro_backend.users.UserMapper;
import dev.ivfrost.hydro_backend.users.UserResolutionService;
import dev.ivfrost.hydro_backend.users.UserResponse;
import dev.ivfrost.hydro_backend.users.UserUpdateRequest;
import dev.ivfrost.hydro_backend.util.PageRequestBuilder;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Users Module", description = "API endpoints for user management and authentication")
@RequiredArgsConstructor
@RestController
@Validated
@RequestMapping("/v1")
public class UserController {

  private final UserService userService;
  private final UserResolutionService userResolutionService;
  private final UserMapper userMapper;
  private final dev.ivfrost.hydro_backend.users.CognitoIdentityResolver cognitoIdentityResolver;

  // ======= NON-AUTHENTICATED USERS ENDPOINTS =======

  @Operation(
      summary = "Check if username or email is available",
      description = "Checks if a username or email is available for registration."
  )
  @GetMapping(value = "/users/validate")
  public ResponseEntity<ApiResponse<Boolean>> validateUsernameEmail(
      @Parameter(description = "Username to check availability for", example = "test_user")
      @RequestParam(required = false) @Size(min = 3, max = 20) String username,
      @Parameter(description = "Email to check availability for", example = "test_email@hydro.com")
      @RequestParam(required = false) @Email String email) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "Validation completed successfully",
            userService.validateUsernameEmail(username, email)
        ));
  }

  // ======= AUTHENTICATED USERS ENDPOINTS =======

  @Operation(
      summary = "Link device to current user",
      description = "Links a device to the currently authenticated user."
  )
  @PostMapping("/me/devices/link")
  public ResponseEntity<ApiResponse<DeviceResponse>> linkDeviceToCurrentUser(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      Authentication authentication,
      @Valid @RequestBody DeviceLinkRequest req) {
    String identityId = cognitoIdentityResolver.resolveIdentityId(idToken(authentication));
    DeviceResponse updatedDevice = userService.linkDeviceToCurrentUser(
        req, authenticatedUser.sub(), identityId, authenticatedUser.id());
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "Device linked successfully", updatedDevice));
  }

  @Operation(
      summary = "Unlink device from current user",
      description = "Unlinks a device from the currently authenticated user."
  )
  @DeleteMapping("/me/devices/unlink")
  public ResponseEntity<ApiResponse<Void>> unlinkDeviceFromCurrentUser(
      @Valid @RequestBody DeviceUnlinkRequest req,
      Authentication authentication,
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
    String identityId = cognitoIdentityResolver.resolveIdentityId(idToken(authentication));
    userService.unlinkDeviceFromCurrentUser(req, identityId, authenticatedUser.id());
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "Device unlinked successfully"));
  }

  private String idToken(Authentication authentication) {
    Object credentials = authentication.getCredentials();
    if (!(credentials instanceof Jwt jwt)) {
      throw new IllegalStateException("Expected a JWT in the authentication credentials");
    }
    return jwt.getTokenValue();
  }

  @Operation(
      summary = "Update device information for current user",
      description = "Updates device information for the currently authenticated user."
  )
  @PatchMapping("/me/devices/{deviceKey}")
  public ResponseEntity<ApiResponse<DeviceResponse>> updateDeviceForCurrentUser(
      @Parameter(description = "Target device key", example = "HYDRO-A7EDS4")
      @PathVariable String deviceKey,
      @Valid @RequestBody DeviceUpdateRequest req) {
    DeviceResponse updatedDevice = userService.updateDeviceForCurrentUser(deviceKey, req);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "Device updated successfully", updatedDevice));
  }

  @Operation(
      summary = "Update devices display order for current user",
      description = "Persists UI user devices display order for the currently authenticated user."
  )
  @PutMapping("/me/devices/display-order")
  public ResponseEntity<ApiResponse<DeviceResponse>> updateDeviceDisplayOrderForCurrentUser(
      @Parameter(description = "List of device IDs in desired display order", example = "[102, 101, 103]")
      @RequestParam @NotEmpty List<@Positive Long> displayOrder) {
    userService.persistDeviceOrderForCurrentUser(displayOrder);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "Devices display order updated successfully"));
  }

  @Operation(
      summary = "Retrieve devices linked to current user",
      description = "Retrieves all devices linked to the currently authenticated user."
  )
  @GetMapping("/me/devices")
  public ResponseEntity<ApiResponse<List<DeviceResponse>>> getDevicesForCurrentUser() {
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "User devices retrieved successfully",
            userService.getDevicesForCurrentUser()));
  }

  @Operation(
      summary = "Get authenticated user's profile",
      description = "Retrieves the profile of the currently authenticated user."
  )
  @GetMapping("/me")
  public ResponseEntity<ApiResponse<UserResponse>> getCurrentUserProfile() {
    return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(HttpStatus.OK,
        "User profile retrieved successfully", userService.getCurrentUserProfile()));
  }

  @Operation(
      summary = "Update user's account settings",
      description = "Updates the account settings of the currently authenticated user."
  )
  @PatchMapping(value = "/me")
  public ResponseEntity<ApiResponse<UserResponse>> updateCurrentUser(
      @Valid @RequestBody UserUpdateRequest userUpdateRequest, Authentication authentication) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "User profile updated successfully",
            userService.updateCurrentUser(userUpdateRequest, (Jwt) authentication.getCredentials())));
  }

  @Operation(
      summary = "Sync the account with the current Cognito state",
      description = "Marks the account email as verified, and promotes a pending email change "
          + "once Cognito confirms it. The app calls this right after a Cognito sign-in so the "
          + "local row is created or bound before other requests."
  )
  @PostMapping("/verify-sync")
  public ResponseEntity<ApiResponse<UserResponse>> syncVerification(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
    User user = userResolutionService.syncVerificationState(authenticatedUser.sub());
    return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK,
        "User email verified successfully", userMapper.userToUserResponse(user)));
  }

  // ======= ADMIN-ONLY ENDPOINTS =======

  @Hidden
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Link a device to a user by user ID (Admin only)")
  @PostMapping("/users/{userId}/devices/link")
  public ResponseEntity<ApiResponse<DeviceResponse>> adminLinkDevice(
      @Parameter(description = "Target user ID") @PathVariable UUID userId,
      @Valid @RequestBody AdminDeviceLinkRequest req) {
    DeviceResponse device = userService.adminLinkDevice(req, userId);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "Device linked to user successfully", device));
  }

  @Hidden
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Retrieve all user profiles (Admin only)")
  @GetMapping(value = "/users/")
  public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUserProfiles(
      @Parameter(description = "Page number for pagination (1-based index)", example = "1")
      @RequestParam(required = false) Integer page,
      @Parameter(description = "Number of user profiles per page", example = "10")
      @RequestParam(required = false) Integer size) {
    Pageable pageable = PageRequestBuilder.buildPageRequest(page, size, "createdAt", Direction.DESC);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "User profiles retrieved successfully",
            userService.getAllUserProfiles(pageable)));
  }

  @Hidden
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Retrieve user profile by ID (Admin only)")
  @GetMapping("/users/{userId}")
  public ResponseEntity<ApiResponse<UserResponse>> getUserProfileById(
      @Parameter(description = "Target user ID")
      @PathVariable UUID userId) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "User profile retrieved successfully",
            userService.getUserProfileById(userId)));
  }

  @Hidden
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Disable user by ID (Admin only)")
  @DeleteMapping("/users/{userId}")
  public ResponseEntity<ApiResponse<Void>> deleteUserById(
      @Parameter(description = "Target user ID")
      @PathVariable UUID userId) {
    userService.disableUserById(userId);
    return ResponseEntity.status(HttpStatus.NO_CONTENT)
        .body(ApiResponse.success(HttpStatus.NO_CONTENT, "User deleted successfully"));
  }
}