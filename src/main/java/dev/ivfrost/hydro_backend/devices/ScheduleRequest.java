package dev.ivfrost.hydro_backend.devices;

import java.util.List;

public record ScheduleRequest(
    List<TimeWindowRequest> windows
) {}

