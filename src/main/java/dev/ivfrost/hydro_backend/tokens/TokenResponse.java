package dev.ivfrost.hydro_backend.tokens;

import java.time.Instant;

public record TokenResponse(
    String value,
    String type,
    Instant expiryDate,
    long userId) {

}
