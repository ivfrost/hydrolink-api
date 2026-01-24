package dev.ivfrost.hydro_backend.devices;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

public record DeviceLinkRequest(@NotNull Long deviceId, @Nullable String secret) {

}