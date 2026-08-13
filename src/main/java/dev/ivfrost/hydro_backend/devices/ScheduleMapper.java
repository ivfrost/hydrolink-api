package dev.ivfrost.hydro_backend.devices;

import dev.ivfrost.hydro_backend.devices.internal.Schedule;
import dev.ivfrost.hydro_backend.devices.internal.TimeWindow;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ScheduleMapper {
  @Mapping(target = "conflictingWindowIds", ignore = true)
  ScheduleResponse toResponse(Schedule schedule);
  List<ScheduleResponse> toResponse(List<Schedule> schedules);
  TimeWindowResponse toResponse(TimeWindow timeWindow);
  TimeWindow toEntity(TimeWindowRequest timeWindowRequest);
  List<TimeWindow> toEntity(List<TimeWindowRequest> timeWindowRequests);
}
