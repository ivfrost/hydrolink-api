package dev.ivfrost.hydro_backend.devices;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.Builder;

@Builder
public record DeviceResponse(
    @NotNull Long id,
    @NotNull String key,
    @NotNull String macAddress,
    @NotNull String friendlyName,
    @NotNull String locationLabel,
    @NotNull String locationCoordinates,
    @NotNull String firmware,
    @NotNull String technicalName,
    @NotNull String description,
    @NotNull String imageUrl,
    @NotNull String ip,
    @NotNull Instant createdAt,
    @NotNull Instant updatedAt,
    @NotNull Instant linkedAt,
    @NotNull UUID userId,
    @NotNull Integer displayOrder,
    @NotNull Set<PinResponse> pins
) {

}
