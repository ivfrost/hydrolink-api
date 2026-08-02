package dev.ivfrost.hydro_backend.devices;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record MqttAclRequest(
    @NotBlank String username,
    @NotBlank String clientId,
    @NotBlank String topic,
    @PositiveOrZero int action,
    @NotBlank String password
) {}