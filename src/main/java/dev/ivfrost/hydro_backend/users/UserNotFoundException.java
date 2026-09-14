package dev.ivfrost.hydro_backend.users;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException {

  public UserNotFoundException(UUID userId) {
    super("User with ID " + userId + " not found.");
  }

  public UserNotFoundException(String message) {
    super(message);
  }

}
