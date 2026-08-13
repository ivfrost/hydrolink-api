package dev.ivfrost.hydro_backend.devices;

import java.time.LocalTime;

public record TimeWindowResponse(
    Long id,
    int pin,
    TimeWindowStartType startType,
    LocalTime fixedTime,
    Integer linkedPin,
    Integer offsetMinutes,
    int durationMinutes,
    boolean hasConflict
) {}