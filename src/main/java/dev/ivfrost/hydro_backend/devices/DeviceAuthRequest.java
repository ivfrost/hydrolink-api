package dev.ivfrost.hydro_backend.devices;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeviceAuthRequest(
    @Schema(example = "HYDRO-AD23V7", description = "Unique key identifying the device")
    @NotBlank(message = "Device key is required")
    String key,

    @Schema(example = "a8aafc3836a93d0d9cb947feb0c721f9", description = "Secret key for authenticating the device")
    @Size(max = 32, message = "Device secret must not exceed 32 characters")
    @NotBlank(message = "Device secret is required")
    String secret
) {

}
