package dev.ivfrost.hydro_backend;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.ObjectMapper;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(LocalDateTime timestamp, int status, String error, String code,
                             String message, T details) {

  private static ObjectMapper objectMapper;

  public static void setObjectMapper(ObjectMapper mapper) {
    objectMapper = mapper;
  }


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

  public static <T> ApiResponse<T> error(HttpStatus status, String code, String message) {
    return new ApiResponse<>(
        LocalDateTime.now(), status.value(), status.getReasonPhrase(), code, message, null);
  }

  public static <T> ApiResponse<T> error(HttpStatus status, String code, String message, T details) {
    return new ApiResponse<>(
        LocalDateTime.now(), status.value(), status.getReasonPhrase(), code, message, details);
  }

  public String toJson() {
    return objectMapper.writeValueAsString(this);
  }
}