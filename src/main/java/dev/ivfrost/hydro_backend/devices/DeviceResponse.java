package dev.ivfrost.hydro_backend.devices;

import java.time.Instant;

public record DeviceResponse(
    Long id,
    String name,
    String location,
    String firmware,
    String technicalName,
    String ip,
    Instant createdAt,
    Instant updatedAt,
    Instant linkedAt,
    Instant lastSeen,
    Long userId,
    Long order) {

}
