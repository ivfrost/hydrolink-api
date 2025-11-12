package dev.ivfrost.hydro_backend.exception;

import jakarta.validation.constraints.Size;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long userId) {
        super("User with ID " + userId + " not found.");
    }

    public UserNotFoundException(String email) {
        super("User with email '" + email + "' not found.");
    }
}
