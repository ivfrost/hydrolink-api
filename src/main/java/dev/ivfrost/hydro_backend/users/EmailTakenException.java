package dev.ivfrost.hydro_backend.users;

public class EmailTakenException extends RuntimeException {

  public EmailTakenException(String email) {
    super("Email '" + email + "' is already taken.");
  }

}
