package dev.ivfrost.hydro_backend.config;

public final class ErrorCodes {
  // Auth / credentials
  public static final String BAD_CREDENTIALS = "BAD_CREDENTIALS";
  public static final String NOT_AUTHENTICATED = "NOT_AUTHENTICATED";
  public static final String ACCESS_DENIED = "ACCESS_DENIED";

  // Users
  public static final String USERNAME_TAKEN = "USERNAME_TAKEN";

  // Tokens (JWT + verification/recovery tokens)
  public static final String TOKEN_NOT_FOUND = "TOKEN_NOT_FOUND";
  public static final String TOKEN_EXPIRED = "TOKEN_EXPIRED";
  public static final String TOKEN_CREATION_FAILED = "TOKEN_CREATION_FAILED";
  public static final String TOKEN_INVALID = "TOKEN_INVALID";
  public static final String RECOVERY_TOKEN_NOT_FOUND = "RECOVERY_TOKEN_NOT_FOUND";
  public static final String RECOVERY_TOKEN_MISMATCH = "RECOVERY_TOKEN_MISMATCH";

  // Devices
  public static final String DEVICE_NOT_FOUND = "DEVICE_NOT_FOUND";
  public static final String DEVICE_LINK_FAILED = "DEVICE_LINK_FAILED";
  public static final String DEVICE_FETCH_FAILED = "DEVICE_FETCH_FAILED";
  public static final String DUPLICATE_MAC_ADDRESS = "DUPLICATE_MAC_ADDRESS";

  // Validation
  public static final String VALIDATION_FAILED = "VALIDATION_FAILED";

  private ErrorCodes() {
    // constants only
  }
}