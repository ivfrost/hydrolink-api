package dev.ivfrost.hydro_backend.devices;

import jakarta.validation.constraints.Size;

public record DeviceProvisionRequest(
    @Size(max = 20)
    String firmware,
    @Size(max = 40)
    String technicalName,
    @Size(max = 17, min = 17)
    String macAddress) {

}
