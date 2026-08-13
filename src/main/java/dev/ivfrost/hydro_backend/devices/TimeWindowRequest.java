package dev.ivfrost.hydro_backend.devices;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record TimeWindowRequest(
    @NotNull Integer pin,
    @NotNull TimeWindowStartType startType,
    LocalTime fixedTime,
    Integer linkedPin,
    LinkedReferencePoint linkedReferencePoint,
    Integer offsetMinutes,
    @NotNull @Min(1) Integer durationMinutes
) {}
