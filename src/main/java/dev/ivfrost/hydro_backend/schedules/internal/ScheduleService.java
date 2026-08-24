package dev.ivfrost.hydro_backend.schedules.internal;

import dev.ivfrost.hydro_backend.devices.DeviceNotFoundException;
import dev.ivfrost.hydro_backend.devices.DeviceResponse;
import dev.ivfrost.hydro_backend.devices.ScheduleDeviceProvider;
import dev.ivfrost.hydro_backend.devices.internal.Device;
import dev.ivfrost.hydro_backend.devices.internal.DeviceRepository;
import dev.ivfrost.hydro_backend.schedules.ScheduleMapper;
import dev.ivfrost.hydro_backend.devices.ScheduleNotFoundException;
import dev.ivfrost.hydro_backend.schedules.ScheduleRequest;
import dev.ivfrost.hydro_backend.schedules.ScheduleResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
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
  private final ScheduleDeviceProvider scheduleDeviceProvider;
//  private final SchedulePublisher schedulePublisher;

  /**
   * Retrieves the schedule for a specific device and day of the week.
   *
   * @param deviceKey the unique key of the device for which the schedule is to be retrieved
   * @param dayOfWeek the day of the week for which the schedule is to be retrieved
   * @return the schedule for the specified device and day of the week
   * @throws DeviceNotFoundException if the device with the given key does not exist
   * @throws ScheduleNotFoundException if the schedule for the given device and day of week does not exist
   */
  public ScheduleResponse getSchedule(String deviceKey, DayOfWeek dayOfWeek) {
    scheduleDeviceProvider.requireDeviceByKey(deviceKey);
    Schedule schedule = scheduleRepository.findByDeviceKeyAndDayOfWeek(deviceKey, dayOfWeek)
        .orElseThrow(() -> new ScheduleNotFoundException(deviceKey, dayOfWeek));
    return enrich(schedule);
  }

  /**
   * Retrieves all schedules for a specific device.
   *
   * @param deviceKey the unique key of the device for which the schedules are to be retrieved
   * @return a list of schedules for the specified device
   * @throws DeviceNotFoundException if the device with the given key does not exist
   * @throws ScheduleNotFoundException if no schedules exist for the given device
   */
  public List<ScheduleResponse> getSchedules(String deviceKey) {
    scheduleDeviceProvider.requireDeviceByKey(deviceKey);
    List<Schedule> schedules = scheduleRepository.findByDeviceKey(deviceKey);
    return schedules.stream().map(this::enrich).toList();
  }

  /**
   * Creates or updates a schedule for a specific device and day of the week.
   * If a schedule already exists for the given device and day, it will be updated with the new
   * time windows provided in the request. If no schedule exists, a new one will be created.
   *
   * @param deviceKey the unique key of the device for which the schedule is to be created or updated
   * @param dayOfWeek the day of the week for which the schedule is to be created or updated
   * @param request the schedule request containing the time windows to be set for the schedule
   * @return the created or updated schedule response
   * @throws DeviceNotFoundException if the device with the given key does not exist
   */
  @Transactional
  public ScheduleResponse upsertSchedule(String deviceKey, DayOfWeek dayOfWeek, ScheduleRequest request) {
    Schedule schedule = scheduleRepository.findByDeviceKeyAndDayOfWeek(deviceKey, dayOfWeek)
        .orElseGet(() -> {
          DeviceResponse deviceResponse = scheduleDeviceProvider.getDeviceByKey(deviceKey);
          return new Schedule(null, dayOfWeek, deviceResponse.key(), new ArrayList<>());
        });

    schedule.getWindows().clear();
    schedule.getWindows().addAll(scheduleMapper.toEntity(request.windows()));
    schedule.getWindows().forEach(w -> w.setSchedule(schedule));

    scheduleRepository.save(schedule);
    // TODO: Send schedules to the device via MQTT
//    schedulePublisher.publish(deviceKey, schedule);

    return enrich(schedule);
  }


  /**
   * Deletes the schedule for a specific device and day of the week.
   *
   * @param deviceKey the unique key of the device for which the schedule is to be deleted
   * @param dayOfWeek the day of the week for which the schedule is to be deleted
   * @throws DeviceNotFoundException if the device with the given key does not exist
   * @throws ScheduleNotFoundException if the schedule for the given device and day of week
   */
  @Transactional
  public void deleteSchedule(String deviceKey, DayOfWeek dayOfWeek) {
    Schedule schedule = scheduleRepository.findByDeviceKeyAndDayOfWeek(deviceKey, dayOfWeek)
        .orElseThrow(() -> new ScheduleNotFoundException(deviceKey, dayOfWeek));
    scheduleRepository.delete(schedule);
  }

  /**
   * Enriches the given schedule with additional information, such as detecting conflicting time
   * windows and builds the ScheduleResponse object to be returned to the client.
   *
   * @param schedule the schedule to be enriched
   * @return the enriched ScheduleResponse object containing the schedule details and any
   * conflicting window IDs
   */
  private ScheduleResponse enrich(Schedule schedule) {
    ScheduleResponse response = scheduleMapper.toResponse(schedule);
    List<Long> conflictingIds = conflictService.detectConflicts(schedule.getWindows());
    return new ScheduleResponse(response.id(), response.dayOfWeek(), response.windows(), conflictingIds);
  }
}