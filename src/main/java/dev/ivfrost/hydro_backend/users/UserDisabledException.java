package dev.ivfrost.hydro_backend.users;

import java.util.UUID;

public class UserDisabledException extends RuntimeException {

  public UserDisabledException(String email) {
    super("User with email " + email + " is disabled");
  }

  public UserDisabledException(UUID userId) {
    super("User with id " + userId + " is disabled");
  }

}
