package dev.ivfrost.hydro_backend;

import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;

public record ApiResponse<T>(LocalDateTime timestamp, int status, String error, String code,
                             String message, T details) {

  public static <T> ApiResponse<T> success(HttpStatus status, String message) {
    return new ApiResponse<>(LocalDateTime.now(), status.value(), null, null, message, null);
  }

  public static <T> ApiResponse<T> success(HttpStatus status, String message, T details) {
    return new ApiResponse<>(LocalDateTime.now(), status.value(), null, null, message, details);
  }

  public static <T> ApiResponse<T> error(HttpStatus status, String message) {
    return new ApiResponse<>(
        LocalDateTime.now(), status.value(), status.getReasonPhrase(), null, message, null);
  }

  public static <T> ApiResponse<T> error(HttpStatus status, String message, T details) {
    return new ApiResponse<>(
        LocalDateTime.now(), status.value(), status.getReasonPhrase(), null, message, details);
  }

  // New: machine-readable code for programmatic handling on clients
  public static <T> ApiResponse<T> error(HttpStatus status, String code, String message) {
    return new ApiResponse<>(
        LocalDateTime.now(), status.value(), status.getReasonPhrase(), code, message, null);
  }

  public static <T> ApiResponse<T> error(HttpStatus status, String code, String message, T details) {
    return new ApiResponse<>(
        LocalDateTime.now(), status.value(), status.getReasonPhrase(), code, message, details);
  }

  public String toJson() {
    return String.format(
        """
            {
              "timestamp": "%s",
              "status": %d,
              "error": "%s",
              "code": "%s",
              "message": "%s",
              "details": "%s"
            }
            """,
        timestamp, status, error, code, message,
        details != null ? details.toString() : "null"
    );
  }
}