package dev.ivfrost.hydro_backend.controller;

import dev.ivfrost.hydro_backend.dto.*;
import dev.ivfrost.hydro_backend.entity.User;
import dev.ivfrost.hydro_backend.service.UserService;
import dev.ivfrost.hydro_backend.util.RateLimitUtils;
import io.github.bucket4j.Bucket;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Tag(name = "User Management", description = "API endpoints for managing users")
@AllArgsConstructor
@RestController
@RequestMapping("/v1")
public class UserController {

    private final UserService userService;
    private final RateLimitUtils rateLimitUtils;

    //======= NON-AUTHENTICATED USERS ENDPOINTS =======//

    // Data modification
    @Operation(
            summary = "Reset user password",
            description = "Resets the user's password using one of the recovery codes provided on registration"
    )
    @PutMapping("/users/password/reset")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody PasswordResetRequest passwordResetConfirmRequest, HttpServletRequest req) {

        Optional<Bucket> bucketOpt = rateLimitUtils
                .getBucketByUserOrIp(userService.getCurrentUser(), RateLimitUtils.extractClientIp(req));
        if (bucketOpt.isEmpty() || !bucketOpt.get().tryConsume(3)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.build(HttpStatus.TOO_MANY_REQUESTS, "", null));
        }
        userService.resetPassword(passwordResetConfirmRequest);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.build(HttpStatus.OK, "Password has been reset successfully", null));
    }


    //======= AUTHENTICATED USERS ENDPOINTS =======//

    // Data retrieval
    @Operation(
            summary = "Get authenticated user's profile",
            description = "Retrieves the profile of the currently authenticated user."
    )
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUserProfile() {
        UserResponse userResponse = userService.getCurrentUserProfile();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.build(HttpStatus.OK, "User profile retrieved successfully", userResponse));
    }

    // Data modification
    @Operation(
            summary = "Update user's account settings",
            description = "Updates the account settings of the currently authenticated user."
    )
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateCurrentUser(
            @Valid @RequestBody UserUpdateRequest userUpdateRequest, HttpServletRequest req) {
        Optional<Bucket> bucketOpt = rateLimitUtils
                .getBucketByUserOrIp(userService.getCurrentUser(), RateLimitUtils.extractClientIp(req));
        if (bucketOpt.isEmpty() || !bucketOpt.get().tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.build(HttpStatus.TOO_MANY_REQUESTS,
                            "", null));
        }
        UserResponse updatedUser = userService.updateCurrentUser(userUpdateRequest);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.build(HttpStatus.OK, "User profile updated successfully", updatedUser));
    }

    // Data removal
    @Operation(
            summary = "Delete authenticated user",
            description = "Deletes the currently authenticated user (soft delete)."
    )
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteCurrentUser(HttpServletRequest req) {

        Optional<Bucket> bucketOpt = rateLimitUtils
                .getBucketByUserOrIp(userService.getCurrentUser(), RateLimitUtils.extractClientIp(req));
        if (bucketOpt.isEmpty() || !bucketOpt.get().tryConsume(2)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.build(HttpStatus.TOO_MANY_REQUESTS,
                            "", null));
        }
        userService.deleteCurrentUser();
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.build(HttpStatus.NO_CONTENT, "User deleted successfully", null));
    }

    //======= ADMIN-ONLY ENDPOINTS =======//

    // Data provision
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Register a new user (Admin only)",
            description = "Creates a new user account at admin's discretion and returns a JWT token. Allows setting user role."
    )
    @PostMapping("/users/new")
    public ResponseEntity<ApiResponse<Void>> registerUsersAdmin(
            @Valid @RequestBody UserRegisterRequest req, @RequestParam(required = false) User.Role role) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.build(HttpStatus.CREATED, "User registered successfully", null));
    }

    // Data retrieval
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get user profile by ID (Admin only)",
            description = "Retrieves a user profile by ID."
    )
    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserProfileById(@PathVariable Long userId) {
        UserResponse userResponse = userService.getUserProfileById(userId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.build(HttpStatus.OK, "User profile retrieved successfully", userResponse));
    }

    // Data removal
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Delete user by ID (Admin only)",
            description = "Deletes a user by ID (soft delete)."
    )
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUserById(@PathVariable Long userId) {
        userService.deleteUserById(userId);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.build(HttpStatus.NO_CONTENT, "User deleted successfully", null));
    }
}
