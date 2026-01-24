package dev.ivfrost.hydro_backend;

import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;

public record ApiResponse<T>(LocalDateTime timestamp, int status, String error, String message,
                             T details) {

  public static <T> ApiResponse<T> success(HttpStatus status, String message) {
    return new ApiResponse<>(
        LocalDateTime.now(),
        status.value(),
        null,
        message,
        null
    );
  }

  public static <T> ApiResponse<T> success(HttpStatus status, String message, T details) {
    return new ApiResponse<>(
        LocalDateTime.now(),
        status.value(),
        null,
        message,
        details
    );
  }

  public static <T> ApiResponse<T> error(HttpStatus status, String message) {
    return new ApiResponse<>(
        LocalDateTime.now(),
        status.value(),
        status.getReasonPhrase(),
        message,
        null
    );
  }

  public static <T> ApiResponse<T> error(HttpStatus status, String message, T details) {
    return new ApiResponse<>(
        LocalDateTime.now(),
        status.value(),
        status.getReasonPhrase(),
        message,
        details
    );
  }

  public String toJson() {
    return String.format(
        """
            {
              "timestamp": "%s",
              "status": %d,
              "error": "%s",
              "message": "%s",
              "details": "%s"
            }
            """,
        timestamp,
        status,
        error,
        message,
        details != null ? details.toString() : "null"
    );
  }
}
