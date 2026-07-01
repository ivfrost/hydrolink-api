package dev.ivfrost.hydro_backend.devices;

import jakarta.validation.constraints.Positive;

public record DeviceUpdateRequest(
    String friendlyName,
    String technicalName,
    String firmware,
    Long userId,
    @Positive Long displayOrder
) {

}
