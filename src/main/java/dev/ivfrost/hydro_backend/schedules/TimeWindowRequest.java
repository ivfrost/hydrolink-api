package dev.ivfrost.hydro_backend.schedules;

import dev.ivfrost.hydro_backend.devices.LinkedReferencePoint;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

@Schema(description = """
    A time window within a schedule. Conditional fields:
    - When startType is FIXED, fixedTime is required.
    - When startType is RELATIVE, linkedPin, linkedReferencePoint and offsetMinutes are required.
    """)
public record TimeWindowRequest(
    @NotNull Integer pin,
    @NotNull TimeWindowStartType startType,
    @Schema(description = "Required when startType is FIXED.", example = "08:30")
    LocalTime fixedTime,
    @Schema(description = "Required when startType is RELATIVE.", example = "1")
    Integer linkedPin,
    @Schema(description = "Required when startType is RELATIVE.", example = "START")
    LinkedReferencePoint linkedReferencePoint,
    @Schema(description = "Required when startType is RELATIVE.", example = "15")
    Integer offsetMinutes,
    @NotNull @Min(1)
    Integer durationMinutes
) {}
