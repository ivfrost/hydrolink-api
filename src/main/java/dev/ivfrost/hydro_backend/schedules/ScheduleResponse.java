package dev.ivfrost.hydro_backend.schedules;

import java.time.DayOfWeek;
import java.util.List;

public record ScheduleResponse(
    Long id,
    DayOfWeek dayOfWeek,
    List<TimeWindowResponse> windows,
    List<Long> conflictingWindowIds
) {}