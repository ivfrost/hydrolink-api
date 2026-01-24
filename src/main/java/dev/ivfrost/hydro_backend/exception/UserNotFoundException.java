package dev.ivfrost.hydro_backend.exception;

public class UserNotFoundException extends RuntimeException {

  public UserNotFoundException(Long userId) {
    super("User with ID " + userId + " not found.");
  }

  public UserNotFoundException(String email) {
    super("User with email '" + email + "' not found.");
  }

}
