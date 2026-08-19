package dev.ivfrost.hydro_backend.devices;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotNull;


public record DeviceUnlinkRequest(
    @Schema(
        description = "The unique key of the device to be unlinked",
        example = "HYDRO-A7EDS4",
        requiredMode = RequiredMode.REQUIRED
    )
    @NotNull
    String deviceKey) {

}
