package dev.ivfrost.hydro_backend.config;

public final class ErrorCodes {
  // Auth / credentials
  public static final String BAD_CREDENTIALS = "BAD_CREDENTIALS";
  public static final String ACCESS_DENIED = "ACCESS_DENIED";
  public static final String EMAIL_NOT_VERIFIED = "EMAIL_NOT_VERIFIED";
  public static final String REAUTHENTICATION_REQUIRED = "REAUTHENTICATION_REQUIRED";

  // Users
  public static final String USERNAME_TAKEN = "USERNAME_TAKEN";
  public static final String USER_NOT_FOUND = "USER_NOT_FOUND";

  // Devices
  public static final String DEVICE_NOT_FOUND = "DEVICE_NOT_FOUND";
  public static final String DEVICE_LINK_FAILED = "DEVICE_LINK_FAILED";
  public static final String DEVICE_FETCH_FAILED = "DEVICE_FETCH_FAILED";
  public static final String SCHEDULE_NOT_FOUND = "SCHEDULE_NOT_FOUND";
  public static final String PINS_NOT_PERSISTED = "PINS_NOT_PERSISTED";
  public static final String PINS_NOT_PROVIDED = "PINS_NOT_PROVIDED";

  // Storage
  public static final String FILE_UPLOAD_EXCEPTION = "FILE_UPLOAD_EXCEPTION";
  public static final String FILE_DOWNLOAD_EXCEPTION = "FILE_DOWNLOAD_EXCEPTION";

  // Validation
  public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
  public static final String JSON_PROCESSING_ERROR = "JSON_PROCESSING_ERROR";
  public static final String JSON_MAPPING_ERROR = "JSON_MAPPING_ERROR";

  // Cognito
  public static final String COGNITO_USER_NOT_FOUND = "COGNITO_USER_NOT_FOUND";
  public static final String COGNITO_MISCONFIGURED = "COGNITO_MISCONFIGURED";
  public static final String COGNITO_THROTTLED = "COGNITO_THROTTLED";
  public static final String COGNITO_ERROR = "COGNITO_ERROR";
  public static final String AWS_UNAVAILABLE = "AWS_UNAVAILABLE";

  // Others
  public static final String IO_ERROR = "IO_ERROR";

  private ErrorCodes() {
    // constants only
  }
}