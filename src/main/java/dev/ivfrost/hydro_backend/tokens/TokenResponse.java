package dev.ivfrost.hydro_backend.tokens;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record TokenResponse(
    @NotNull
    String value,
    @NotNull
    String type,
    @NotNull
    Instant expiryDate,
    @NotNull
    UUID userId) {

}
