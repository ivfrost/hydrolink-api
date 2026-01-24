package dev.ivfrost.hydro_backend.devices;

import jakarta.validation.constraints.NotBlank;

public record DeviceAuthRequest(
    @NotBlank(message = "Device ID is required")
    Long deviceId,

    @NotBlank(message = "Device secret is required")
    String secret
) {

}
