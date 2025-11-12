package dev.ivfrost.hydro_backend.exception;

public class AuthWrongPasswordException extends RuntimeException {
    public AuthWrongPasswordException(String email) {
        super("Wrong password for user with email: " + email);
    }
}
