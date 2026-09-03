package dev.ivfrost.hydro_backend.devices;

import jakarta.validation.constraints.NotNull;

public record PinRequest(
    @NotNull
    Integer pinNumber,
    @NotNull
    PinMode mode
) {}