package dev.ivfrost.hydro_backend.users;

public class ReauthenticationRequiredException extends RuntimeException {
  public ReauthenticationRequiredException() {
    super("You must re-authenticate in order to proceed with this action");
  }

}
