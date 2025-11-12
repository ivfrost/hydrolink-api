package dev.ivfrost.hydro_backend.exception;

public class UserDeletedException extends RuntimeException {

    public UserDeletedException(Long userId) {
        super("User with ID " + userId + " is deleted.");
    }
}
