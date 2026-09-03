package dev.ivfrost.hydro_backend.schedules;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

public record ScheduleResponse(
    Long id,
    LocalDate date,
    List<TimeWindowResponse> windows,
    List<Long> conflictingWindowIds
) {}