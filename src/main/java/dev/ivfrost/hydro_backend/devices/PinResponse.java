package dev.ivfrost.hydro_backend.devices;

import java.time.Instant;
import lombok.Builder;

@Builder
public record PinResponse(
    Integer pinNumber,
    PinMode mode,
    Instant updatedAt
) {}