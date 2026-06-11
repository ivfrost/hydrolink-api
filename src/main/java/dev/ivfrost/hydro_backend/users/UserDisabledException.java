package dev.ivfrost.hydro_backend.users;

public class UserDisabledException extends RuntimeException {

  public UserDisabledException(String email) {
    super("User with email " + email + " is disabled");
  }

  public UserDisabledException(Long userId) {
    super("User with id " + userId + " is disabled");
  }

}
