package dev.ivfrost.hydro_backend.controller;

import dev.ivfrost.hydro_backend.dto.*;
import dev.ivfrost.hydro_backend.service.UserService;
import dev.ivfrost.hydro_backend.util.RateLimitUtils;
import io.github.bucket4j.Bucket;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springdoc.webmvc.core.service.RequestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@Tag(name = "User Authentication", description = "API endpoints for user authentication")
@AllArgsConstructor
@RestController
@RequestMapping("/v1/")
public class AuthController {

    private final UserService userService;
    private final RateLimitUtils rateLimitUtils;
    private final RequestService requestService;

    //======= NON-AUTHENTICATED USERS ENDPOINTS =======//

    // Data provision
    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account."
    )
    @PostMapping("/users")
    public ResponseEntity<ApiResponse<UserRegisterResponse>> registerUser(
            @Valid @RequestBody UserRegisterRequest userRegisterRequest, HttpServletRequest req) {

        Optional<Bucket> bucketOpt = rateLimitUtils
                .getBucketByUserOrIp(userService.getCurrentUser(), RateLimitUtils.extractClientIp(req));
        if (bucketOpt.isEmpty() || !bucketOpt.get().tryConsume(5)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.build(HttpStatus.TOO_MANY_REQUESTS, "Too many requests - rate limit exceeded", null));
        }

        UserRegisterResponse recoveryCodes = userService.addUser(userRegisterRequest);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.build(HttpStatus.CREATED, "User registered successfully", recoveryCodes));
    }

    @Operation(
            summary = "Authenticate user",
            description = "Authenticates a user and returns a JWT token."
    )
    @PostMapping("/users/auth")
    public ResponseEntity<ApiResponse<AuthResponse>> authenticateUser(
            @Valid @RequestBody UserLoginRequest userLoginRequest, HttpServletRequest req) {

        Optional<Bucket> bucketOpt = rateLimitUtils
                .getBucketByUserOrIp(userService.getCurrentUser(), RateLimitUtils.extractClientIp(req));
        if (bucketOpt.isEmpty() || !bucketOpt.get().tryConsume(2)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.build(HttpStatus.TOO_MANY_REQUESTS, "", null));
        }

        AuthResponse authResponse = userService.authenticateUser(userLoginRequest);
        ApiResponse<AuthResponse> response = ApiResponse.build(HttpStatus.OK, "User authenticated successfully", authResponse);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Refresh JWT token",
            description = "Refreshes the JWT token for an authenticated user."
    )
    @PostMapping("/users/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken() {
        AuthResponse authResponse = userService.refreshTokens();
        ApiResponse<AuthResponse> response = ApiResponse.build(HttpStatus.OK, "Token refreshed successfully", authResponse);
        return ResponseEntity.ok(response);
    }
}
