package dev.ivfrost.hydro_backend.devices;

import jakarta.validation.constraints.Positive;

public record DeviceUpdateRequest(
    String friendlyName,
    String technicalName,
    String firmware,
    String location,
    String description,
    String imageUrl,
    @Positive Long userId,
    @Positive Long displayOrder
) {

}
