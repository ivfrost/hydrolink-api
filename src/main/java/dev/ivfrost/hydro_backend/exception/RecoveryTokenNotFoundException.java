package dev.ivfrost.hydro_backend.exception;

public class RecoveryTokenNotFoundException extends RuntimeException {
    public RecoveryTokenNotFoundException(String message) {
        super(message);
    }
}
