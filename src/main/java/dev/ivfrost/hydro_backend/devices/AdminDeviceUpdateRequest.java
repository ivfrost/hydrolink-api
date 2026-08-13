package dev.ivfrost.hydro_backend.devices;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AdminDeviceUpdateRequest(
    @Size(max = 40)
    String friendlyName,
    @Size(max = 40)
    String technicalName,
    @Size(max = 20)
    String firmware,
    @Size(max = 255)
    String locationLabel,
    @Size(max = 255)
    String locationCoordinates,
    @Size(max = 255)
    String description,
    @Size(max = 2048)
    String imageUrl,
    Long userId,
    @Positive Long displayOrder
) {

}
