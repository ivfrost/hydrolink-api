package dev.ivfrost.hydro_backend.config;

import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import dev.ivfrost.hydro_backend.ApiResponse;
import dev.ivfrost.hydro_backend.devices.DeviceFetchException;
import dev.ivfrost.hydro_backend.devices.DeviceLinkException;
import dev.ivfrost.hydro_backend.devices.DeviceNotFoundException;
import dev.ivfrost.hydro_backend.devices.DuplicateMacAddressException;
import dev.ivfrost.hydro_backend.tokens.ExpiredVerificationToken;
import dev.ivfrost.hydro_backend.tokens.RecoveryTokenMismatchException;
import dev.ivfrost.hydro_backend.tokens.RecoveryTokenNotFoundException;
import dev.ivfrost.hydro_backend.tokens.TokenNotFoundException;
import dev.ivfrost.hydro_backend.users.UserDisabledException;
import dev.ivfrost.hydro_backend.users.UserNotAuthenticatedException;
import dev.ivfrost.hydro_backend.users.UsernameTakenException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(UserDisabledException.class)
  public ResponseEntity<ApiResponse<Void>> handleUserDisabledException(
      UserDisabledException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        // Don't expose the reason for authentication failure to prevent user enumeration attacks
        .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
  }

  @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleUserNotFoundException(
      AuthenticationCredentialsNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, ex.getMessage()));
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(
      BadCredentialsException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, ex.getMessage()));
  }

  @ExceptionHandler(UserNotAuthenticatedException.class)
  public ResponseEntity<ApiResponse<Void>> handleUserNotAuthenticatedException(
      UserNotAuthenticatedException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, ex.getMessage()));
  }

  @ExceptionHandler(UsernameTakenException.class)
  public ResponseEntity<ApiResponse<Void>> handleUsernameTakenException(
      UsernameTakenException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ApiResponse.error(HttpStatus.CONFLICT, ex.getMessage()));
  }

  @ExceptionHandler(TokenNotFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleTokenNotFoundException(
      TokenNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiResponse.error(HttpStatus.NOT_FOUND, ex.getMessage()));
  }

  @ExceptionHandler(ExpiredVerificationToken.class)
  public ResponseEntity<ApiResponse<Void>> handleExpiredVerificationToken(
      ExpiredVerificationToken ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error(HttpStatus.BAD_REQUEST, ex.getMessage()));
  }

  @ExceptionHandler(JWTCreationException.class)
  public ResponseEntity<ApiResponse<Void>> handleJWTCreationException(
      JWTCreationException ex) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage()));
  }

  @ExceptionHandler(JWTVerificationException.class)
  public ResponseEntity<ApiResponse<Void>> handleJWTVerificationException(
      JWTVerificationException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, ex.getMessage()));
  }

  @ExceptionHandler(DeviceNotFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleDeviceNotFoundException(
      DeviceNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiResponse.error(HttpStatus.NOT_FOUND, ex.getMessage()));
  }

  @ExceptionHandler(DeviceLinkException.class)
  public ResponseEntity<ApiResponse<Void>> handleDeviceLinkException(
      DeviceLinkException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error(HttpStatus.BAD_REQUEST, ex.getMessage()));
  }

  @ExceptionHandler(DeviceFetchException.class)
  public ResponseEntity<ApiResponse<Void>> handleDeviceFetchException(
      DeviceFetchException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiResponse.error(HttpStatus.NOT_FOUND, ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
      MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult()
        .getFieldErrors()
        .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            ApiResponse.error(HttpStatus.BAD_REQUEST, "Validation failed for one or more fields.",
                errors));
  }

  @ExceptionHandler(RecoveryTokenNotFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleRecoveryCodeNotFoundException(
      RecoveryTokenNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiResponse.error(HttpStatus.NOT_FOUND, ex.getMessage()));
  }

  @ExceptionHandler(RecoveryTokenMismatchException.class)
  public ResponseEntity<ApiResponse<Void>> handleRecoveryCodeMismatchException(
      RecoveryTokenMismatchException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error(HttpStatus.BAD_REQUEST, ex.getMessage()));
  }

  @ExceptionHandler(DuplicateMacAddressException.class)
  public ResponseEntity<ApiResponse<Void>> handleDuplicateMacAddressException(
      DuplicateMacAddressException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ApiResponse.error(HttpStatus.CONFLICT, ex.getMessage()));
  }
}
