package dev.ivfrost.hydro_backend.exception;

public class ExpiredVerificationToken extends RuntimeException {
    public ExpiredVerificationToken(String message) {
        super(message);
    }
}
