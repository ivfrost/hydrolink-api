package dev.ivfrost.hydro_backend.users;

public class EmailNotVerifiedException extends RuntimeException {

  public EmailNotVerifiedException(String s) {
    super(s);
  }
}
