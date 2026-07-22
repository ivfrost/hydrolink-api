package dev.ivfrost.hydro_backend;

import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

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

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
      .registerModule(new JavaTimeModule());

  public String toJson() {
    try {
      return OBJECT_MAPPER.writeValueAsString(this);
    } catch (JsonProcessingException e) {
      // Fallback in case serialization fails
      return String.format("{\"status\":%d, \"message\":\"%s\"}", status, message);
    }
  }
}