package dev.ivfrost.hydro_backend.service;

import dev.ivfrost.hydro_backend.entity.UserToken;
import dev.ivfrost.hydro_backend.exception.*;
import dev.ivfrost.hydro_backend.dto.*;
import dev.ivfrost.hydro_backend.entity.User;
import dev.ivfrost.hydro_backend.repository.UserTokenRepository;
import dev.ivfrost.hydro_backend.repository.UserRepository;
import dev.ivfrost.hydro_backend.security.JWTUtil;
import dev.ivfrost.hydro_backend.util.DeviceDtoUtil;
import dev.ivfrost.hydro_backend.util.RecoveryCodeUtil;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserTokenRepository userTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EncoderService encoderService;
    private final JWTUtil jwtUtil;
    @Value("${security-code.secret}")
    private String securityCodeSecret;

    public UserService(UserRepository userRepository, UserTokenRepository userTokenRepository, PasswordEncoder passwordEncoder, EncoderService encoderService, JWTUtil jwtUtil) {
        this.userRepository = userRepository;
        this.userTokenRepository = userTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.encoderService = encoderService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Adds a new user with a specified role (admin only).
     * Recovery codes are generated and the user is prompted to save them securely.
     * @param req the user registration request DTO
     * @param role the role to assign to the user
     * @throws UsernameTakenException if the username is already taken
     * @return the user registration response containing recovery codes
     */
    @Transactional
    public UserRegisterResponse addUser(UserRegisterRequest req, User.Role role) {
        if (userRepository.findByUsername(req.getUsername()).isPresent()) {
            throw new UsernameTakenException(req.getUsername());
        }
        User user = convertRequestToUser(req);
        user.setRole(role != null ? role : User.Role.USER); // Default to USER role if not specified
        userRepository.save(user);
        String[] recoveryCodes = RecoveryCodeUtil.generateRecoveryCodes(5);
        for (String code : recoveryCodes) {
            String encodedCode = encoderService.hmacSha256Encoder().apply(securityCodeSecret, code);

            UserToken token = new UserToken();
            token.setType(UserToken.TokenType.RECOVERY_CODE);
            token.setToken(encodedCode);
            token.setExpiryDate(null);
            token.setUser(user);
            userTokenRepository.save(token);
        }
        return new UserRegisterResponse(recoveryCodes);
    }

    /**
     * Adds a new user with the default USER role (for self-registration).
     * Recovery codes are generated and the user is prompted to save them securely.
     * The first registered user is assigned the ADMIN role
     * @param req the user registration request DTO
     * @throws UsernameTakenException if the username is already taken
     * @return the user registration response containing recovery codes
     */
    @Transactional
    public UserRegisterResponse addUser(UserRegisterRequest req) {
        boolean isFirstUser = userRepository.count() == 0;
        User.Role role = isFirstUser ? User.Role.ADMIN : User.Role.USER; // First user is ADMIN, others are USER
        return addUser(req, role);
    }

    /**
     * Authenticates a user by email and password, returning a JWT token and refresh token if successful.
     * @param req the user login request DTO
     * @return the authentication response containing the JWT token and refresh token
     * @throws UserNotFoundException if the user is not found
     * @throws AuthWrongPasswordException if the password is incorrect
     */
    public AuthResponse authenticateUser(UserLoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new UserNotFoundException(req.getEmail()));
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new AuthWrongPasswordException(req.getEmail());
        }

        user.setLastLogin(LocalDateTime.now().atZone(ZoneOffset.UTC).toInstant());
        user.setActive(true);
        userRepository.save(user);
        return buildAuthResponse(user, "JWT token and refresh token generated successfully.");
    }

    /**
     * Refreshes the JWT token and rotates the refresh token for the currently authenticated user.
     * @return the authentication response containing the new JWT token and refresh token
     * @throws UserNotFoundException if the user is not found
     */
    public AuthResponse refreshTokens() throws UserNotFoundException {
        User user = getCurrentUserWithoutDevices();
        return buildAuthResponse(user, "JWT token and refresh token refreshed successfully.");
    }

    /**
     * Retrieves the currently authenticated user without devices (for performance).
     * @return the authenticated user entity
     * @throws UserNotFoundException if the user is not found
     */
    public User getCurrentUserWithoutDevices() {
        Long userId = getCurrentUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    /**
     * Retrieves the currently authenticated user with devices, or null if not authenticated.
     * @return the authenticated user entity with devices, or null if not authenticated
     */
    public User getCurrentUser() {
        if (!isUserAuthenticated()) {
            return null;
        }
        try {
            Long userId = getCurrentUserId();
            return userRepository.findByIdWithDevices(userId).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Retrieves the profile of the currently authenticated user as a response DTO.
     * @return the user response DTO, or null if not authenticated
     */
    public UserResponse getCurrentUserProfile() {
        return convertUserToResponse(getCurrentUser());
    }

    /**
     * Retrieves a user by their unique ID (admin only, without devices).
     * @param userId the ID of the user to retrieve
     * @return the user entity
     * @throws UserNotFoundException if the user is not found
     */
    public User getUserByIdWithoutDevices(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }

    /**
     * Retrieves a user by their unique ID with devices (admin only).
     * @param userId the ID of the user to retrieve
     * @return the user entity with devices
     * @throws UserNotFoundException if the user is not found
     */
    public User getUserById(Long userId) {
        return userRepository.findByIdWithDevices(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }

    /**
     * Retrieves a user profile by their unique ID as a response DTO (admin only).
     * @param userId the ID of the user to retrieve
     * @return the user response DTO
     * @throws UserNotFoundException if the user is not found
     */
    public UserResponse getUserProfileById(Long userId) throws UserNotFoundException {
        User user = getUserById(userId);
        return convertUserToResponse(user);
    }

    /**
     * Deletes the currently authenticated user (soft delete).
     * @throws IllegalStateException if no authenticated user is found
     */
    public void deleteCurrentUser() throws IllegalStateException {
        Long userId = getCurrentUserId();
        deleteUserById(userId);
    }

    /**
     * Deletes a user by their unique ID (admin only, soft delete).
     * @param userId the ID of the user to delete
     * @throws UserDeletedException if the user is already deleted
     * @throws UserNotFoundException if the user is not found
     */
    public void deleteUserById(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        if (user.isDeleted()) {
            throw new UserDeletedException(userId);
        }
        user.setDeleted(true);
        userRepository.save(user);
    }

    /**
     * Resets the user's password using a recovery code provided to the user on registration.
     * @param req the password reset request DTO containing the user email, recovery code and new password
     * @throws RecoveryTokenNotFoundException if the recovery token is not found
     * @throws UserNotFoundException if the user is not found
     * @throws UserDeletedException if the user is deleted
     */
    @Transactional
    public void resetPassword(PasswordResetRequest req) {
        String encodedCode = encoderService.hmacSha256Encoder().apply(securityCodeSecret, req.getRecoveryCode());
        UserToken recoveryToken = userTokenRepository
                .findByTokenAndType(encodedCode, UserToken.TokenType.RECOVERY_CODE)
                .orElseThrow(() -> new RecoveryTokenNotFoundException("Invalid recovery code."));

        User user = recoveryToken.getUser();
        if (user == null) {
            throw new UserNotFoundException("User associated with the token not found.");
        }
        if (user.isDeleted()) {
            throw new UserDeletedException(user.getId());
        }
        if (!user.getEmail().equals(req.getEmail())) {
            throw new RecoveryTokenMismatchException("Recovery code does not match the provided email.");
        }
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
        // Invalidate the used token
        userTokenRepository.delete(recoveryToken);
    }

    /**
     * Update the currently authenticated user's account settings.
     * @param req the user update request DTO containing the fields to update
     * @return the updated user response DTO
     * @throws IllegalStateException if no authenticated user is found
     * @throws UserNotFoundException if the user is not found
     * @throws UserDeletedException if the user is deleted
     */
    @Transactional
    public UserResponse updateCurrentUser(UserUpdateRequest req) {
        Long userId = getCurrentUserId();
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        if (user.isDeleted()) {
            throw new UserDeletedException(userId);
        }
        if (req.getFullName() != null && !req.getFullName().isBlank()) {
            user.setFullName(req.getFullName());
        }
        if (req.getEmail() != null && !req.getEmail().isBlank()) {
            user.setEmail(req.getEmail());
        }
        if (req.getPhoneNumber() != null) {
            user.setPhoneNumber(req.getPhoneNumber());
        }
        if (req.getAddress() != null) {
            user.setAddress(req.getAddress());
        }
        if (req.getProfilePictureUrl() != null) {
            user.setProfilePictureUrl(req.getProfilePictureUrl());
        }
        if (req.getPreferredLanguage() != null && !req.getPreferredLanguage().isBlank()) {
            user.setPreferredLanguage(req.getPreferredLanguage());
        }
        if (req.getSettings() != null) {
            user.setSettings(req.getSettings());
        }
        userRepository.save(user);
        return convertUserToResponse(user);
    }

    /*--------------------------*/
    /*      Helper Methods      */
    /*--------------------------*/

    /**
     * Builds an AuthResponse with both tokens and a message.
     * @param user the user entity
     * @param message the message to include
     * @return the AuthResponse containing both tokens and the message
     */
    private AuthResponse buildAuthResponse(User user, String message) {
        String token = generateToken(user);
        String refreshToken = generateRefreshToken(user);
        return new AuthResponse(token, refreshToken, message);
    }

    /**
     * Checks if a user is authenticated in the security context.
     * @return true if a user is authenticated, false otherwise
     */
    private boolean isUserAuthenticated() {
        SecurityContext context = SecurityContextHolder.getContext();
        return context.getAuthentication() != null && context.getAuthentication().isAuthenticated();
    }

    /**
     * Retrieves the ID of the currently authenticated user from the security context.
     * @return the user ID
     * @throws IllegalStateException if no authenticated user is found
     */
    private Long getCurrentUserId() {
        if (isUserAuthenticated()) {
            String name = SecurityContextHolder.getContext().getAuthentication().getName();
            if ("anonymousUser".equals(name)) {
                throw new IllegalStateException("No authenticated user found in security context.");
            }
            return Long.parseLong(name);
        } else {
            throw new IllegalStateException("No authenticated user found in security context.");
        }
    }

    /**
     * Converts a UserRegisterRequest DTO to a User entity.
     * @param req the user registration request DTO
     * @return the user entity
     */
    private User convertRequestToUser(UserRegisterRequest req) {
        String encodedPassword = passwordEncoder.encode(req.getPassword()); // Bcrypt password encoding
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(encodedPassword);
        user.setEmail(req.getEmail());
        user.setFullName(req.getFullName());
        user.setPreferredLanguage(req.getPreferredLanguage());
        return user;
    }

    /**
     * Converts a User entity to a UserResponse DTO.
     * @param user the user entity
     * @return the user response DTO
     */
    private UserResponse convertUserToResponse(User user) {
        if (user == null) {
            return null;
        }

        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setProfilePictureUrl(user.getProfilePictureUrl());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setAddress(user.getAddress());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        response.setLastLogin(user.getLastLogin());
        response.setRole(user.getRole());
        response.setPreferredLanguage(user.getPreferredLanguage());
        response.setSettings(user.getSettings());

        List<DeviceResponse> userDevices = user.getDevices() != null ?
            DeviceDtoUtil.convertDevicesToResponse(user.getDevices()) :
            Collections.emptyList();
        response.setDevices(userDevices);

        return response;
    }

    /**
     * Generates a JWT token for a given user.
     * @param user the user entity
     * @return a String containing the JWT token
     * @throws UserDeletedException if the user is deleted
     */
    private String generateToken(User user)  {
        if (user.isDeleted()) {
            throw new UserDeletedException(user.getId());
        }
        return jwtUtil.generateToken(user);
    }

    /**
     * Generates a refresh JWT token for a given user.
     * @param user the user entity
     * @return a String containing the refresh JWT token
     * @throws UserDeletedException if the user is deleted
     */
    private String generateRefreshToken(User user) {
        if (user.isDeleted()) {
            throw new UserDeletedException(user.getId());
        }
        return jwtUtil.generateRefreshToken(user);
    }
}
