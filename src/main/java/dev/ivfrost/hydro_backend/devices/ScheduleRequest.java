package dev.ivfrost.hydro_backend.devices;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record ScheduleRequest(
    @Schema(description = "List of time windows for the schedule", requiredMode = Schema.RequiredMode.REQUIRED)
    List<TimeWindowRequest> windows
) {}

