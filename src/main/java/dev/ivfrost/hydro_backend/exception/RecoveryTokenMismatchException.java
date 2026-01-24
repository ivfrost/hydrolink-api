package dev.ivfrost.hydro_backend.exception;

public class RecoveryTokenMismatchException extends RuntimeException {

  public RecoveryTokenMismatchException(String message) {
    super(message);
  }

}
