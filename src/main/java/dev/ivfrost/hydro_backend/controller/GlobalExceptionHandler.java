package dev.ivfrost.hydro_backend.controller;

import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import dev.ivfrost.hydro_backend.dto.ApiResponse;
import dev.ivfrost.hydro_backend.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserDeletedException.class)
    public ResponseEntity<ApiResponse<LocalDateTime>> handleUserDeletedException(UserDeletedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ApiResponse.build(HttpStatus.FORBIDDEN, ex.getMessage(), LocalDateTime.now())
        );
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<LocalDateTime>> handleUserNotFoundException(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse.build(HttpStatus.NOT_FOUND, ex.getMessage(), LocalDateTime.now())
        );
    }

    @ExceptionHandler(UserNotAuthenticatedException.class)
    public ResponseEntity<ApiResponse<LocalDateTime>> handleUserNotAuthenticatedException(UserNotAuthenticatedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ApiResponse.build(HttpStatus.UNAUTHORIZED, ex.getMessage(), LocalDateTime.now())
        );
    }

    @ExceptionHandler(TokenNotFoundException.class)
    public ResponseEntity<ApiResponse<LocalDateTime>> handleTokenNotFoundException(TokenNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse.build(HttpStatus.NOT_FOUND, ex.getMessage(), LocalDateTime.now())
        );
    }

    @ExceptionHandler(ExpiredVerificationToken.class)
    public ResponseEntity<ApiResponse<LocalDateTime>> handleExpiredVerificationToken(ExpiredVerificationToken ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse.build(HttpStatus.BAD_REQUEST, ex.getMessage(), LocalDateTime.now())
        );
    }

    @ExceptionHandler(JWTCreationException.class)
    public ResponseEntity<ApiResponse<LocalDateTime>> handleJWTCreationException(JWTCreationException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.build(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), LocalDateTime.now())
        );
    }

    @ExceptionHandler(JWTVerificationException.class)
    public ResponseEntity<ApiResponse<LocalDateTime>>  handleJWTVerificationException(JWTVerificationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ApiResponse.build(HttpStatus.UNAUTHORIZED, ex.getMessage(), LocalDateTime.now())
        );
    }

    @ExceptionHandler(DeviceLinkException.class)
    public ResponseEntity<ApiResponse<LocalDateTime>> handleDeviceLinkException(DeviceLinkException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse.build(HttpStatus.BAD_REQUEST, ex.getMessage(), LocalDateTime.now())
        );
    }

    @ExceptionHandler(DeviceFetchException.class)
    public ResponseEntity<ApiResponse<LocalDateTime>> handleDeviceFetchException(DeviceFetchException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse.build(HttpStatus.NOT_FOUND, ex.getMessage(), LocalDateTime.now())
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(MethodArgumentNotValidException ex) {
        // Collect field errors into a map
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            errors.put(error.getField(), error.getDefaultMessage())
        );
        String message = "Validation failed for one or more fields.";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse.build(HttpStatus.BAD_REQUEST, message, errors)
        );
    }

    @ExceptionHandler(RecoveryTokenNotFoundException.class)
    public ResponseEntity<ApiResponse<LocalDateTime>> handleRecoveryCodeNotFoundException(
            RecoveryTokenNotFoundException ex) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse.build(HttpStatus.NOT_FOUND, ex.getMessage(), LocalDateTime.now())
        );
    }

    @ExceptionHandler(RecoveryTokenMismatchException.class)
    public ResponseEntity<ApiResponse<LocalDateTime>> handleRecoveryCodeMismatchException(
            RecoveryTokenMismatchException ex) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse.build(HttpStatus.BAD_REQUEST, ex.getMessage(), LocalDateTime.now())
        );
    }
}
