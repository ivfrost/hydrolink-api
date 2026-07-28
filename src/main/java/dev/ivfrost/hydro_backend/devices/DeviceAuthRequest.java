package dev.ivfrost.hydro_backend.devices;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeviceAuthRequest(
    @NotBlank(message = "Device key is required")
    String key,

    @Size(max = 255)
    @NotBlank(message = "Device secret is required")
    String secret
) {

}
