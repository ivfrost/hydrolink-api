package dev.ivfrost.hydro_backend.devices;

import jakarta.validation.constraints.NotNull;

public record DeviceUnlinkRequest(@NotNull String deviceKey) {

}
