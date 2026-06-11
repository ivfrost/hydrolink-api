package dev.ivfrost.hydro_backend.users.internal;

import dev.ivfrost.hydro_backend.ApiResponse;
import dev.ivfrost.hydro_backend.devices.DeviceLinkRequest;
import dev.ivfrost.hydro_backend.devices.DeviceResponse;
import dev.ivfrost.hydro_backend.tokens.TokenResponse;
import dev.ivfrost.hydro_backend.users.UserAuthRequest;
import dev.ivfrost.hydro_backend.users.UserMqttResponse;
import dev.ivfrost.hydro_backend.users.UserRecoveryRequest;
import dev.ivfrost.hydro_backend.users.UserRegisterRequest;
import dev.ivfrost.hydro_backend.users.UserResponse;
import dev.ivfrost.hydro_backend.users.UserUpdateRequest;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import org.apache.hc.core5.http.HttpHeaders;
import org.jspecify.annotations.Nullable;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Users Module", description = "API endpoints for user management and authentication")
@AllArgsConstructor
@RestController
@RequestMapping("/v1")
public class UserController {

  private final UserService userService;
  private final Environment environment;

  // ======= NON-AUTHENTICATED USERS ENDPOINTS =======

  @Operation(
      summary = "Authenticate user",
      description = "Authenticates a user and returns auth and refresh JWT tokens."
  )
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = @Content(
          examples = @ExampleObject(
              name = "UserAuthRequest example",
              value = """
                  {
                    "email": "admin@hydro.com",
                    "password": "admin"
                  }
                  """,
              summary = "Example of a user authentication request"
          )
      )
  )
  @PostMapping("/users/auth")
  public ResponseEntity<ApiResponse<TokenResponse>> authenticateUser(
      @Valid @RequestBody UserAuthRequest userAuthRequest) {
    var tokens = userService.authenticateUser(userAuthRequest);
    var accessToken = tokens.stream().filter(t -> t.type().equals("AUTH_ACCESS_TOKEN")).findFirst().orElseThrow();
    var refreshToken = tokens.stream().filter(t -> t.type().equals("AUTH_REFRESH_TOKEN")).findFirst().orElseThrow();
    ResponseCookie refreshTokenCookie = generateRefreshTokenCookie(refreshToken, "/v1/users/auth/refresh");
    return ResponseEntity.status(HttpStatus.OK)
        .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
        .body(ApiResponse.success(HttpStatus.OK, "User authenticated successfully",
            accessToken
        ));
  }

  @Operation(
      summary = "Register user",
      description = "Registers a user and returns an array of recovery codes"
  )
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = @Content(
          examples = @ExampleObject(
              name = "UserRegisterRequest example",
              value = """
                  {
                    "email": "test_user@hydro.com",
                    "username": "test_user",
                    "fullName": "Test User",
                    "password": "test_user",
                    "preferredLanguage": "en"
                  }
                  """,
              summary = "Example of a user registration request"
          )
      )
  )
  @PostMapping("/users")
  public ResponseEntity<ApiResponse<List<TokenResponse>>> registerUser(
      @Valid @RequestBody UserRegisterRequest userRegisterRequest) {

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(HttpStatus.CREATED, "User registered successfully",
            userService.addUser(userRegisterRequest)
        ));
  }

  @Operation(
      summary = "Check if username or email is available",
      description = "Checks if a username or email is available for registration."
  )
  @GetMapping(value = "/users/validate")
  public ResponseEntity<ApiResponse<Boolean>> validateUsernameEmail(
      @Nullable String username,
      @Nullable String email) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "Validation completed successfully",
            userService.validateUsernameEmail(username, email)
        ));
  }

  // ======= AUTHENTICATED USERS ENDPOINTS =======

  /**
   * Resets the user's password using one of the recovery codes provided on registration.
   */
  @Operation(summary = "Reset user password",
      description = "Resets the user's password using one of the recovery codes provided on registration.")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = @Content(
          examples = @ExampleObject(
              name = "UserRecoveryRequest example",
              value = """
                  {
                    "email": "test_user@hydro.com",
                    "recoveryCode": "abcde12345",
                    "newPassword": "new_secure_password"
                  }
                  """,
              summary = "Example of a user password reset request"
          )
      )
  )
  @PutMapping("/users/password/reset")
  public ResponseEntity<ApiResponse<Void>> resetPassword(
      @Valid @RequestBody UserRecoveryRequest passwordResetConfirmRequest) {
    userService.resetPassword(passwordResetConfirmRequest);
    return ResponseEntity.ok()
        .body(ApiResponse.success(HttpStatus.OK, "Password reset successfully"));
  }

  @Operation(summary = "Get authenticated user's profile",
      description = "Retrieves the profile of the currently authenticated user.")
  @GetMapping("/me")
  public ResponseEntity<ApiResponse<UserResponse>> getCurrentUserProfile() {
    return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(HttpStatus.OK,
        "User profile retrieved successfully", userService.getCurrentUserProfile()));
  }

  /*
   * Updates the account settings of the currently authenticated user.
   */
  @Operation(summary = "Update user's account settings",
      description = "Updates the account settings of the currently authenticated user.")
  @PutMapping(value = "/me", consumes = "multipart/form-data")
  public ResponseEntity<ApiResponse<UserResponse>> updateCurrentUser(
      @RequestPart("data") UserUpdateRequest userUpdateRequest,
      @RequestPart(value = "profilePicture", required = false) MultipartFile profilePicture
  ) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "User profile updated successfully",
            userService.updateCurrentUser(userUpdateRequest, profilePicture)));
  }

  /**
   * Deletes the currently authenticated user (soft delete).
   */
  @Operation(summary = "Delete authenticated user",
      description = "Deletes the currently authenticated user (soft delete).")
  @DeleteMapping("/me")
  public ResponseEntity<ApiResponse<Void>> deleteCurrentUser() {
    userService.deleteCurrentUser();
    return ResponseEntity.status(HttpStatus.NO_CONTENT)
        .body(ApiResponse.success(HttpStatus.NO_CONTENT, "User deleted successfully"));
  }

  /*
   * Retrieves new auth and refresh tokens if current refresh token is valid.
   */
  @Operation(
      summary = "Get user auth JWT token",
      description = "Refreshes the JWT tokens for an authenticated user.")
  @PostMapping("/users/auth/refresh")
  public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(@CookieValue("refreshToken") String refreshToken) {
    var tokens = userService.refreshTokens(refreshToken);
    var accessToken = tokens.stream().filter(t -> t.type().equals("AUTH_ACCESS_TOKEN")).findFirst().orElseThrow();
    var newRefreshToken = tokens.stream().filter(t -> t.type().equals("AUTH_REFRESH_TOKEN")).findFirst().orElseThrow();
    ResponseCookie refreshTokenCookie = generateRefreshTokenCookie(newRefreshToken, "/v1/users/auth/refresh");
    return ResponseEntity.status(HttpStatus.OK)
        .headers(httpHeaders -> httpHeaders.add(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString()))
        .body(ApiResponse.success(HttpStatus.OK, "Tokens refreshed successfully",
            accessToken
        ));
  }

  /*
   * Retrieves a RS256 signed JWT token for MQTT authentication.
   */
  @Operation(
      summary = "Get MQTT auth JWT token",
      description = "Returns a RS256 signed JWT token for MQTT authentication.")
  @GetMapping("/users/auth/mqtt")
  public ResponseEntity<ApiResponse<UserMqttResponse>> getMqttAuthToken() {
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "MQTT auth token retrieved successfully",
            userService.getMqttAuthToken()
        ));
  }

  /*
   * Link device to the currently authenticated user.
   */
  @Operation(
      summary = "Link device to current user",
      description = "Links a device to the currently authenticated user.")
  @PostMapping("/me/devices/link")
  public ResponseEntity<ApiResponse<Void>> linkDeviceToCurrentUser(
      @RequestBody DeviceLinkRequest req) {
    userService.linkDeviceToCurrentUser(req);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "Device linked successfully"));
  }

  /*
   * Unlink device from the currently authenticated user.
   */
  @Operation(
      summary = "Unlink device from current user",
      description = "Unlinks a device from the currently authenticated user.")
  @DeleteMapping("/me/devices/link")
  public ResponseEntity<ApiResponse<Void>> unlinkDeviceFromCurrentUser(
      @Valid @RequestBody DeviceLinkRequest req) {
    userService.unlinkDeviceFromCurrentUser(req);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "Device unlinked successfully"));
  }

  /*
   * Retrieves all devices linked to the currently authenticated user.
   */
  @Operation(
      summary = "Retrieve devices linked to current user",
      description = "Retrieves all devices linked to the currently authenticated user.")
  @GetMapping("/me/devices")
  public ResponseEntity<ApiResponse<List<DeviceResponse>>> getDevicesForCurrentUser() {
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "User devices retrieved successfully",
            userService.getDevicesForCurrentUser()));
  }

  // ======= ADMIN-ONLY ENDPOINTS =======

  /**
   * Creates a new user account. Allows setting user roles.
   */
  @Hidden
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Register a new user (Admin only)",
      description = "Creates a new user account. Allows setting user roles.")
  @PostMapping("/users/new")
  public ResponseEntity<ApiResponse<List<TokenResponse>>> registerUsersAdmin(
      @Valid @RequestBody UserRegisterRequest req,
      @RequestBody List<User.Role> roles) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            ApiResponse.success(HttpStatus.CREATED, "User registered successfully",
                userService.addUser(req, roles)));
  }

  /**
   * Retrieves all user profiles.
   */
  @Hidden
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Retrieve all user profiles (Admin only)")
  @GetMapping(value = "/users/", params = {"page", "size"})
  public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUserProfiles(
      @ParameterObject Pageable pageable) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "User profiles retrieved successfully",
            userService.getAllUserProfiles(pageable)));
  }

  /**
   * Retrieves user profile by ID.
   */
  @Hidden
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Retrieve user profile by ID (Admin only)")
  @GetMapping("/users/{userId}")
  public ResponseEntity<ApiResponse<UserResponse>> getUserProfileById(@PathVariable Long userId) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "User profile retrieved successfully",
            userService.getUserProfileById(userId)));
  }

  /**
   * Deletes user by ID (soft delete)
   */
  @Hidden
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Disable user by ID (Admin only)")
  @DeleteMapping("/users/{userId}")
  public ResponseEntity<ApiResponse<Void>> deleteUserById(@PathVariable Long userId) {
    userService.deleteUserById(userId);
    return ResponseEntity.status(HttpStatus.NO_CONTENT)
        .body(ApiResponse.success(HttpStatus.NO_CONTENT, "User deleted successfully"));
  }

  /**
   * Generates a secure HTTP-only cookie for the refresh token
   */
  private ResponseCookie generateRefreshTokenCookie(TokenResponse refreshToken, String path) {
    return ResponseCookie.from("refreshToken", refreshToken.value())
        .httpOnly(true)
        .secure(!environment.acceptsProfiles(Profiles.of("env")))
        .path(path)
        .maxAge(Duration.between(Instant.now(), refreshToken.expiryDate()))
        .sameSite("Strict")
        .build();
  }

}

