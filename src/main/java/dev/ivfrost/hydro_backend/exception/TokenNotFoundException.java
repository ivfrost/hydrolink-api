package dev.ivfrost.hydro_backend.exception;

import dev.ivfrost.hydro_backend.entity.UserToken;

public class TokenNotFoundException extends RuntimeException {
    public TokenNotFoundException(UserToken.TokenType type) {
        super("Token of type " + type + " not found or invalid.");
    }
}
