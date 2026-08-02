package dev.ivfrost.hydro_backend.devices;

import jakarta.validation.constraints.NotBlank;

public record MqttAuthRequest(
    @NotBlank String username,
    @NotBlank String password,
    @NotBlank String clientId
) {}
