package dev.ivfrost.hydro_backend.devices;

public record DeviceUpdateRequest(

    Long id,
    String friendlyName,
    String technicalName,
    String firmware,
    Long userId,
    Long displayOrder
) {

}
