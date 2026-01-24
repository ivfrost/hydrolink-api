package dev.ivfrost.hydro_backend.exception;

/**
 * Custom exception for encryption/decryption errors
 */
public class EncryptionException extends RuntimeException {

  public EncryptionException(String message) {
    super(message);
  }
}