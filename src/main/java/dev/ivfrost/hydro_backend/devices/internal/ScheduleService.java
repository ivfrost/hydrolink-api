package dev.ivfrost.hydro_backend.devices.internal;

import dev.ivfrost.hydro_backend.devices.DeviceNotFoundException;
import dev.ivfrost.hydro_backend.devices.ScheduleMapper;
import dev.ivfrost.hydro_backend.devices.ScheduleNotFoundException;
import dev.ivfrost.hydro_backend.devices.ScheduleRequest;
import dev.ivfrost.hydro_backend.devices.ScheduleResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
@Tag(name = "Schedule Service", description = "Service for managing device schedules")
@RequiredArgsConstructor
@Service
public class ScheduleService {

  private final ScheduleRepository scheduleRepository;
  private final ScheduleMapper scheduleMapper;
  private final ConflictService conflictService;
  private final DeviceRepository deviceRepository;
//  private final SchedulePublisher schedulePublisher;

  public ScheduleResponse getSchedule(String deviceKey, DayOfWeek dayOfWeek) {
    deviceRepository.findByKey(deviceKey)
        .orElseThrow(() -> new DeviceNotFoundException(deviceKey));
    Schedule schedule = scheduleRepository.findByDeviceKeyAndDayOfWeek(deviceKey, dayOfWeek)
        .orElseThrow(() -> new ScheduleNotFoundException(deviceKey, dayOfWeek));
    return enrich(schedule);
  }

  public List<ScheduleResponse> getSchedules(String deviceKey) {
      deviceRepository.findByKey(deviceKey)
          .orElseThrow(() -> new DeviceNotFoundException(deviceKey));
    List<Schedule> schedules = scheduleRepository.findByDeviceKey(deviceKey);
    return schedules.stream().map(this::enrich).toList();
  }

  public ScheduleResponse upsertSchedule(String deviceKey, DayOfWeek dayOfWeek, ScheduleRequest request) {
    Schedule schedule = scheduleRepository.findByDeviceKeyAndDayOfWeek(deviceKey, dayOfWeek)
        .orElseGet(() -> {
          Device device = deviceRepository.findByKey(deviceKey)
              .orElseThrow(() -> new DeviceNotFoundException(deviceKey));
          return new Schedule(null, dayOfWeek, device, new ArrayList<>());
        });

    schedule.getWindows().clear();
    schedule.getWindows().addAll(scheduleMapper.toEntity(request.windows()));
    schedule.getWindows().forEach(w -> w.setSchedule(schedule));

    scheduleRepository.save(schedule);
    // TODO: Send schedules to the device via MQTT
//    schedulePublisher.publish(deviceKey, schedule);

    return enrich(schedule);
  }

  public void deleteSchedule(String deviceKey, DayOfWeek dayOfWeek) {
    Schedule schedule = scheduleRepository.findByDeviceKeyAndDayOfWeek(deviceKey, dayOfWeek)
        .orElseThrow(() -> new ScheduleNotFoundException(deviceKey, dayOfWeek));
    scheduleRepository.delete(schedule);
  }

  private ScheduleResponse enrich(Schedule schedule) {
    ScheduleResponse response = scheduleMapper.toResponse(schedule);
    List<Long> conflictingIds = conflictService.detectConflicts(schedule.getWindows());
    return new ScheduleResponse(response.id(), response.dayOfWeek(), response.windows(), conflictingIds);
  }
}