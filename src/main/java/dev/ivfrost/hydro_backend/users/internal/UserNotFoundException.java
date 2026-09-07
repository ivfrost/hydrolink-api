package dev.ivfrost.hydro_backend.users.internal;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException {

  public UserNotFoundException(UUID userId) {
    super("User with ID " + userId + " not found.");
  }

  public UserNotFoundException(String email) {
    super("User with email '" + email + "' not found.");
  }

}
