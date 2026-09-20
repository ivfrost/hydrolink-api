package dev.ivfrost.hydro_backend.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import dev.ivfrost.hydro_backend.common.ApiResponse;
import dev.ivfrost.hydro_backend.devices.DeviceFetchException;
import dev.ivfrost.hydro_backend.devices.DeviceLinkException;
import dev.ivfrost.hydro_backend.devices.DeviceNotFoundException;
import dev.ivfrost.hydro_backend.devices.PinsNotPersistedException;
import dev.ivfrost.hydro_backend.devices.PinsNotProvidedException;
import dev.ivfrost.hydro_backend.devices.ScheduleNotFoundException;
import dev.ivfrost.hydro_backend.storage.FileDownloadException;
import dev.ivfrost.hydro_backend.storage.FileUploadException;
import dev.ivfrost.hydro_backend.users.UserDisabledException;
import dev.ivfrost.hydro_backend.users.EmailNotVerifiedException;
import dev.ivfrost.hydro_backend.users.ReauthenticationRequiredException;
import dev.ivfrost.hydro_backend.users.UsernameTakenException;
import jakarta.validation.ConstraintViolationException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.TooManyRequestsException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(UserDisabledException.class)
  public ResponseEntity<ApiResponse<Void>> handleUserDisabledException(
      UserDisabledException ex) {
    log.warn("Disabled user attempted access: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, ErrorCodes.BAD_CREDENTIALS, ex.getMessage()));
  }

  @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleUserNotFoundException(
      AuthenticationCredentialsNotFoundException ex) {
    log.warn("Missing authentication credentials: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, ErrorCodes.BAD_CREDENTIALS, ex.getMessage()));
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(
      BadCredentialsException ex) {
    log.warn("Bad credentials: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, ErrorCodes.BAD_CREDENTIALS, ex.getMessage()));
  }

  @ExceptionHandler(EmailNotVerifiedException.class)
  public ResponseEntity<ApiResponse<Void>> handleEmailNotVerifiedException(
      EmailNotVerifiedException ex) {
    log.warn("Unverified email attempted a protected action: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ApiResponse.error(HttpStatus.FORBIDDEN, ErrorCodes.EMAIL_NOT_VERIFIED, ex.getMessage()));
  }

  @ExceptionHandler(ReauthenticationRequiredException.class)
  public ResponseEntity<ApiResponse<Void>> handleReauthenticationRequiredException(
      ReauthenticationRequiredException ex) {
    log.warn("Sensitive action rejected, authentication too old: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, ErrorCodes.REAUTHENTICATION_REQUIRED,
            ex.getMessage()));
  }

  @ExceptionHandler(UsernameTakenException.class)
  public ResponseEntity<ApiResponse<Void>> handleUsernameTakenException(
      UsernameTakenException ex) {
    log.warn("Username already taken: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ApiResponse.error(HttpStatus.CONFLICT, ErrorCodes.USERNAME_TAKEN, ex.getMessage()));
  }

  @ExceptionHandler(DeviceNotFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleDeviceNotFoundException(
      DeviceNotFoundException ex) {
    log.warn("Device not found: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiResponse.error(HttpStatus.NOT_FOUND, ErrorCodes.DEVICE_NOT_FOUND, ex.getMessage()));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
      AccessDeniedException ex) {
    log.warn("Access denied: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ApiResponse.error(HttpStatus.FORBIDDEN, ErrorCodes.ACCESS_DENIED, ex.getMessage()));
  }

  @ExceptionHandler(DeviceLinkException.class)
  public ResponseEntity<ApiResponse<Void>> handleDeviceLinkException(
      DeviceLinkException ex) {
    log.warn("Device link failed: {}", ex.getMessage());
    return ResponseEntity.badRequest()
        .body(ApiResponse.error(HttpStatus.BAD_REQUEST, ErrorCodes.DEVICE_LINK_FAILED, ex.getMessage()));
  }

  @ExceptionHandler(DeviceFetchException.class)
  public ResponseEntity<ApiResponse<Void>> handleDeviceFetchException(
      DeviceFetchException ex) {
    log.warn("Device fetch failed: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiResponse.error(HttpStatus.NOT_FOUND, ErrorCodes.DEVICE_FETCH_FAILED, ex.getMessage()));
  }

  @ExceptionHandler(FileUploadException.class)
  public ResponseEntity<ApiResponse<Void>> handleStorageException(
      FileUploadException ex) {
    log.error("File upload failed: {}", ex.getMessage(), ex);
    return ResponseEntity.internalServerError()
        .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.FILE_UPLOAD_EXCEPTION, ex.getMessage()));
  }

  @ExceptionHandler(FileDownloadException.class)
  public ResponseEntity<ApiResponse<Void>> handleFileDownloadException(
      FileDownloadException ex) {
    log.error("File download failed: {}", ex.getMessage(), ex);
    return ResponseEntity.internalServerError()
        .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.FILE_DOWNLOAD_EXCEPTION, ex.getMessage()));
  }

  @ExceptionHandler(IOException.class)
  public ResponseEntity<ApiResponse<Void>> handleIOException(IOException ex) {
    log.error("Unhandled IO error: {}", ex.getMessage(), ex);
    return ResponseEntity.internalServerError()
        .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.IO_ERROR, ex.getMessage()));
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(
      MaxUploadSizeExceededException ex) {
    log.warn("Upload exceeded max size: {}", ex.getMessage());
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
    log.warn("JSON processing error: {}", ex.getMessage());
    return ResponseEntity.badRequest()
        .body(ApiResponse.error(HttpStatus.BAD_REQUEST, ErrorCodes.JSON_PROCESSING_ERROR, ex.getMessage()));
  }

  @ExceptionHandler(JsonMappingException.class)
  public ResponseEntity<ApiResponse<Void>> handleJsonMappingException(
      JsonMappingException ex) {
    log.warn("JSON mapping error: {}", ex.getMessage());
    return ResponseEntity.badRequest()
        .body(ApiResponse.error(HttpStatus.BAD_REQUEST, ErrorCodes.JSON_MAPPING_ERROR, ex.getMessage()));
  }

  @ExceptionHandler(ScheduleNotFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleScheduleNotFoundException(
      ScheduleNotFoundException ex) {
    log.warn("Schedule not found: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiResponse.error(HttpStatus.NOT_FOUND, ErrorCodes.SCHEDULE_NOT_FOUND, ex.getMessage()));
  }

  @ExceptionHandler(PinsNotPersistedException.class)
  public ResponseEntity<ApiResponse<Void>> handlePinsNotPersistedException(
      PinsNotPersistedException ex) {
    log.error("Pins not persisted: {}", ex.getMessage(), ex);
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiResponse.error(HttpStatus.NOT_FOUND, ErrorCodes.PINS_NOT_PERSISTED, ex.getMessage()));
  }

  @ExceptionHandler(PinsNotProvidedException.class)
  public ResponseEntity<ApiResponse<Void>> handlePinsNotProvidedException(
      PinsNotProvidedException ex) {
    log.warn("Pins not provided: {}", ex.getMessage());
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
    log.warn("Validation failed: {}", errors);

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
    log.warn("Constraint violation: {}", errors);

    return ResponseEntity.badRequest()
        .body(ApiResponse.error(
            HttpStatus.BAD_REQUEST,
            ErrorCodes.VALIDATION_FAILED,
            "Constraint validation failed.",
            errors
        ));
  }

  // AWS Cognito
  @ExceptionHandler(UserNotFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleCognitoUserNotFound(UserNotFoundException ex) {
    log.warn("Cognito user not found for a validated token, sub may have been deleted: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, ErrorCodes.COGNITO_USER_NOT_FOUND,
            "Session is no longer valid. Please sign in again."));
  }

  @ExceptionHandler(dev.ivfrost.hydro_backend.users.UserNotFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleDomainUserNotFound(
      dev.ivfrost.hydro_backend.users.UserNotFoundException ex) {
    log.warn("User resolution failed: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ApiResponse.error(HttpStatus.CONFLICT, ErrorCodes.USER_NOT_FOUND, ex.getMessage()));
  }

  @ExceptionHandler(NotAuthorizedException.class)
  public ResponseEntity<ApiResponse<Void>> handleCognitoNotAuthorized(NotAuthorizedException ex) {
    log.error("Cognito call not authorized, check IAM credentials/policy for AdminGetUser: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.COGNITO_MISCONFIGURED,
            "Unable to complete sign-in at this time."));
  }

  @ExceptionHandler(TooManyRequestsException.class)
  public ResponseEntity<ApiResponse<Void>> handleCognitoThrottled(TooManyRequestsException ex) {
    log.warn("Cognito request throttled: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(ApiResponse.error(HttpStatus.SERVICE_UNAVAILABLE, ErrorCodes.COGNITO_THROTTLED,
            "Sign-in service is busy. Please try again shortly."));
  }

  @ExceptionHandler(CognitoIdentityProviderException.class)
  public ResponseEntity<ApiResponse<Void>> handleCognitoGeneric(CognitoIdentityProviderException ex) {
    log.error("Unhandled Cognito error: {}", ex.getMessage(), ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.COGNITO_ERROR,
            "Unable to complete sign-in at this time."));
  }

  @ExceptionHandler(SdkClientException.class)
  public ResponseEntity<ApiResponse<Void>> handleAwsClientUnreachable(SdkClientException ex) {
    log.error("Could not reach AWS (network/SDK client issue): {}", ex.getMessage(), ex);
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(ApiResponse.error(HttpStatus.SERVICE_UNAVAILABLE, ErrorCodes.AWS_UNAVAILABLE,
            "Sign-in service is temporarily unavailable."));
  }
}