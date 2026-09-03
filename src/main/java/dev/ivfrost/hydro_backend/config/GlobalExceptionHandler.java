package dev.ivfrost.hydro_backend.config;

import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import dev.ivfrost.hydro_backend.ApiResponse;
import dev.ivfrost.hydro_backend.devices.DeviceFetchException;
import dev.ivfrost.hydro_backend.devices.DeviceLinkException;
import dev.ivfrost.hydro_backend.devices.DeviceNotFoundException;
import dev.ivfrost.hydro_backend.devices.DuplicateMacAddressException;
import dev.ivfrost.hydro_backend.devices.PinsNotPersistedException;
import dev.ivfrost.hydro_backend.devices.PinsNotProvidedException;
import dev.ivfrost.hydro_backend.devices.ScheduleNotFoundException;
import dev.ivfrost.hydro_backend.storage.FileDownloadException;
import dev.ivfrost.hydro_backend.storage.FileUploadException;
import dev.ivfrost.hydro_backend.tokens.ExpiredVerificationToken;
import dev.ivfrost.hydro_backend.tokens.RecoveryTokenMismatchException;
import dev.ivfrost.hydro_backend.tokens.RecoveryTokenNotFoundException;
import dev.ivfrost.hydro_backend.tokens.TokenNotFoundException;
import dev.ivfrost.hydro_backend.users.UserDisabledException;
import dev.ivfrost.hydro_backend.users.UserNotAuthenticatedException;
import dev.ivfrost.hydro_backend.users.UsernameTakenException;
import jakarta.validation.ConstraintViolationException;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(UserDisabledException.class)
  public ResponseEntity<ApiResponse<Void>> handleUserDisabledException(
      UserDisabledException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, ErrorCodes.BAD_CREDENTIALS, ex.getMessage()));
  }

  @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleUserNotFoundException(
      AuthenticationCredentialsNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, ErrorCodes.BAD_CREDENTIALS, ex.getMessage()));
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(
      BadCredentialsException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, ErrorCodes.BAD_CREDENTIALS, ex.getMessage()));
  }

  @ExceptionHandler(UserNotAuthenticatedException.class)
  public ResponseEntity<ApiResponse<Void>> handleUserNotAuthenticatedException(
      UserNotAuthenticatedException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, ErrorCodes.NOT_AUTHENTICATED, ex.getMessage()));
  }

  @ExceptionHandler(UsernameTakenException.class)
  public ResponseEntity<ApiResponse<Void>> handleUsernameTakenException(
      UsernameTakenException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ApiResponse.error(HttpStatus.CONFLICT, ErrorCodes.USERNAME_TAKEN, ex.getMessage()));
  }

  @ExceptionHandler(TokenNotFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleTokenNotFoundException(
      TokenNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiResponse.error(HttpStatus.NOT_FOUND, ErrorCodes.TOKEN_NOT_FOUND, ex.getMessage()));
  }

  @ExceptionHandler(ExpiredVerificationToken.class)
  public ResponseEntity<ApiResponse<Void>> handleExpiredVerificationToken(
      ExpiredVerificationToken ex) {
    return ResponseEntity.badRequest()
        .body(ApiResponse.error(HttpStatus.BAD_REQUEST, ErrorCodes.TOKEN_EXPIRED, ex.getMessage()));
  }

  @ExceptionHandler(JWTCreationException.class)
  public ResponseEntity<ApiResponse<Void>> handleJWTCreationException(
      JWTCreationException ex) {
    return ResponseEntity.internalServerError()
        .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.TOKEN_CREATION_FAILED, ex.getMessage()));
  }

  @ExceptionHandler(JWTVerificationException.class)
  public ResponseEntity<ApiResponse<Void>> handleJWTVerificationException(
      JWTVerificationException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, ErrorCodes.TOKEN_INVALID, ex.getMessage()));
  }

  @ExceptionHandler(DeviceNotFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleDeviceNotFoundException(
      DeviceNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiResponse.error(HttpStatus.NOT_FOUND, ErrorCodes.DEVICE_NOT_FOUND, ex.getMessage()));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
      AccessDeniedException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ApiResponse.error(HttpStatus.FORBIDDEN, ErrorCodes.ACCESS_DENIED, ex.getMessage()));
  }

  @ExceptionHandler(DeviceLinkException.class)
  public ResponseEntity<ApiResponse<Void>> handleDeviceLinkException(
      DeviceLinkException ex) {
    return ResponseEntity.badRequest()
        .body(ApiResponse.error(HttpStatus.BAD_REQUEST, ErrorCodes.DEVICE_LINK_FAILED, ex.getMessage()));
  }

  @ExceptionHandler(DeviceFetchException.class)
  public ResponseEntity<ApiResponse<Void>> handleDeviceFetchException(
      DeviceFetchException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiResponse.error(HttpStatus.NOT_FOUND, ErrorCodes.DEVICE_FETCH_FAILED, ex.getMessage()));
  }

  @ExceptionHandler(RecoveryTokenNotFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleRecoveryCodeNotFoundException(
      RecoveryTokenNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiResponse.error(HttpStatus.NOT_FOUND, ErrorCodes.RECOVERY_TOKEN_NOT_FOUND, ex.getMessage()));
  }

  @ExceptionHandler(RecoveryTokenMismatchException.class)
  public ResponseEntity<ApiResponse<Void>> handleRecoveryCodeMismatchException(
      RecoveryTokenMismatchException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error(HttpStatus.BAD_REQUEST, ErrorCodes.RECOVERY_TOKEN_MISMATCH, ex.getMessage()));
  }

  @ExceptionHandler(DuplicateMacAddressException.class)
  public ResponseEntity<ApiResponse<Void>> handleDuplicateMacAddressException(
      DuplicateMacAddressException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ApiResponse.error(HttpStatus.CONFLICT, ErrorCodes.DUPLICATE_MAC_ADDRESS, ex.getMessage()));
  }

  @ExceptionHandler(FileUploadException.class)
  public ResponseEntity<ApiResponse<Void>> handleStorageException(
      FileUploadException ex) {
    return ResponseEntity.internalServerError()
        .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.FILE_UPLOAD_EXCEPTION, ex.getMessage()));
  }

  @ExceptionHandler(FileDownloadException.class)
  public ResponseEntity<ApiResponse<Void>> handleFileDownloadException(
      FileDownloadException ex) {
    return ResponseEntity.internalServerError()
        .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.FILE_DOWNLOAD_EXCEPTION, ex.getMessage()));
  }

  @ExceptionHandler(IOException.class)
  public ResponseEntity<ApiResponse<Void>> handleIOException(IOException ex) {
    return ResponseEntity.internalServerError()
        .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.IO_ERROR, ex.getMessage()));
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(
      MaxUploadSizeExceededException ex) {
    return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE)
        .body(ApiResponse.error(
            HttpStatus.CONTENT_TOO_LARGE,
            ErrorCodes.FILE_UPLOAD_EXCEPTION,
            "Upload exceeds the maximum allowed size (8MB). Please provide a smaller firmware file."
        ));
  }

  @ExceptionHandler(JsonProcessingException.class)
  public ResponseEntity<ApiResponse<Void>> handleJsonProcessingException(
      JsonProcessingException ex) {
    return ResponseEntity.badRequest()
        .body(ApiResponse.error(HttpStatus.BAD_REQUEST, ErrorCodes.JSON_PROCESSING_ERROR, ex.getMessage()));
  }

  @ExceptionHandler(JsonMappingException.class)
  public ResponseEntity<ApiResponse<Void>> handleJsonMappingException(
      JsonMappingException ex) {
    return ResponseEntity.badRequest()
        .body(ApiResponse.error(HttpStatus.BAD_REQUEST, ErrorCodes.JSON_MAPPING_ERROR, ex.getMessage()));
  }

  @ExceptionHandler(ScheduleNotFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleScheduleNotFoundException(
      ScheduleNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiResponse.error(HttpStatus.NOT_FOUND, ErrorCodes.SCHEDULE_NOT_FOUND, ex.getMessage()));
  }

  @ExceptionHandler(PinsNotPersistedException.class)
  public ResponseEntity<ApiResponse<Void>> handlePinsNotPersistedException(
      PinsNotPersistedException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiResponse.error(HttpStatus.NOT_FOUND, ErrorCodes.PINS_NOT_PERSISTED, ex.getMessage()));
  }

  @ExceptionHandler(PinsNotProvidedException.class)
  public ResponseEntity<ApiResponse<Void>> handlePinsNotProvidedException(
      PinsNotProvidedException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error(HttpStatus.BAD_REQUEST, ErrorCodes.PINS_NOT_PROVIDED, ex.getMessage()));
  }

  // Handles @Valid on @RequestBody / @RequestPart DTOs
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
      MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult().getFieldErrors().forEach(error ->
        errors.put(error.getField(), error.getDefaultMessage())
    );

    return ResponseEntity.badRequest()
        .body(ApiResponse.error(
            HttpStatus.BAD_REQUEST,
            ErrorCodes.VALIDATION_FAILED,
            "Validation failed for one or more fields.",
            errors
        ));
  }

  // Handles @Validated on Method Parameters, @RequestParam, @PathVariable
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiResponse<Map<String, String>>> handleConstraintViolationException(
      ConstraintViolationException ex) {
    Map<String, String> errors = new HashMap<>();

    ex.getConstraintViolations().forEach(cv -> {
      String propertyPath = cv.getPropertyPath().toString();
      // Clean up "updateCurrentUser.req.email" -> "email"
      String fieldName = propertyPath.substring(propertyPath.lastIndexOf('.') + 1);
      errors.put(fieldName, cv.getMessage());
    });

    return ResponseEntity.badRequest()
        .body(ApiResponse.error(
            HttpStatus.BAD_REQUEST,
            ErrorCodes.VALIDATION_FAILED,
            "Constraint validation failed.",
            errors
        ));
  }

}
