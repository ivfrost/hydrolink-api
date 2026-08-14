package dev.ivfrost.hydro_backend.devices;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

@Schema(description = """
    A time window within a schedule. Conditional fields:
    - When startType is FIXED, fixedTime is required.
    - When startType is RELATIVE, linkedPin, linkedReferencePoint and offsetMinutes are required.
    These conditional requirements are enforced server-side and cannot be expressed as
    OpenAPI schema constraints.
    """)
public record TimeWindowRequest(
    @NotNull Integer pin,
    @NotNull TimeWindowStartType startType,
    @Schema(description = "Required when startType is FIXED.")
    LocalTime fixedTime,
    @Schema(description = "Required when startType is RELATIVE.")
    Integer linkedPin,
    @Schema(description = "Required when startType is RELATIVE.")
    LinkedReferencePoint linkedReferencePoint,
    @Schema(description = "Required when startType is RELATIVE.")
    Integer offsetMinutes,
    @NotNull @Min(1) Integer durationMinutes
) {}
