package dev.ivfrost.hydro_backend.controller;

import dev.ivfrost.hydro_backend.dto.*;
import dev.ivfrost.hydro_backend.repository.UserTokenRepository;
import dev.ivfrost.hydro_backend.service.UserService;
import dev.ivfrost.hydro_backend.service.UserTokenService;
import dev.ivfrost.hydro_backend.util.RateLimitUtils;
import dev.ivfrost.hydro_backend.util.ValidationUtils;
import io.github.bucket4j.Bucket;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Tag(name = "Validation ", description = "API endpoints for validating data")
@AllArgsConstructor
@RequestMapping("/v1/validation")
@RestController
public class ValidationController {

    private final UserTokenRepository userTokenRepository;
    ValidationUtils validationUtils;
    HashMap<String, Bucket> buckets;
    RateLimitUtils rateLimitUtils;
    UserService userService;
    UserTokenService userTokenService;

    @Operation(summary = "Get validation rules for a specific class")
    @GetMapping("/rules")
    public ResponseEntity<?> getClassValidationRules(@RequestParam String className, HttpServletRequest req) {
        Optional<Bucket> bucketOpt = rateLimitUtils
                .getBucketByUserOrIp(userService.getCurrentUser(), RateLimitUtils.extractClientIp(req));
        if (bucketOpt.isEmpty() || !bucketOpt.get().tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.build(HttpStatus.TOO_MANY_REQUESTS, "", null));
        }

        Map<String, Object> rules;
        String message;
        switch (className) {
            case "UserRegisterRequest" -> {
                rules = validationUtils.getClassValidationRules(UserRegisterRequest.class);
                message = "User register validation rules";
            }
            case "UserLoginRequest" -> {
                rules = validationUtils.getClassValidationRules(UserLoginRequest.class);
                message = "User login validation rules";
            }
            case "DeviceProvisionRequest" -> {
                rules = validationUtils.getClassValidationRules(DeviceProvisionRequest.class);
                message = "Device provision validation rules";
            }
            case "DeviceLinkRequest" -> {
                rules = validationUtils.getClassValidationRules(DeviceLinkRequest.class);
                message = "Device link validation rules";
            }
            default -> {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.build(HttpStatus.BAD_REQUEST, "Invalid field", null));
            }
        }
        return ResponseEntity.ok(ApiResponse.build(HttpStatus.OK, message, rules));
    }

    @Operation(summary = "Get availability of a username or email")
    @GetMapping("/availability")
    public ResponseEntity<?> checkUsernameEmailAvailability(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            HttpServletRequest req) {

        if (username == null && email == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.build(HttpStatus.BAD_REQUEST, "Either username or email must be provided", null));
        }
        if (username != null && email != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.build(HttpStatus.BAD_REQUEST, "Only one of username or email must be provided", null));
        }
        boolean isAvailable;
        String field;
        if (username != null) {
            isAvailable = validationUtils.isUsernameAvailable(username);
            field = "username";
        } else {
            isAvailable = validationUtils.isEmailAvailable(email);
            field = "email";
        }
        String message = isAvailable ? field + " is available" : field + " is already taken";
        return ResponseEntity.ok(ApiResponse.build(HttpStatus.OK, message, isAvailable));
    }

    @Operation(summary = "Get validity of recovery code for a given email")
    @PostMapping("/recovery-code")
    public ResponseEntity<?> checkRecoveryCodeValidity(@RequestParam String rawCode, @RequestParam String email,
                                                       HttpServletRequest req) {

        Optional<Bucket> bucketOpt = rateLimitUtils
                .getBucketByUserOrIp(userService.getCurrentUser(), RateLimitUtils.extractClientIp(req));
        if (bucketOpt.isEmpty() || !bucketOpt.get().tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.build(HttpStatus.TOO_MANY_REQUESTS, "Too many requests - rate limit exceeded", null));
        }

        boolean isValid = userTokenService.isRecoveryCodeValid(rawCode, email);
        String message = isValid ? "Recovery code is valid" : "Invalid recovery code or email";
        return ResponseEntity.ok(ApiResponse.build(HttpStatus.OK, message, isValid));
    }

    @Operation(summary = "Health check endpoint")
    @GetMapping("/health")
    @ResponseBody
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
