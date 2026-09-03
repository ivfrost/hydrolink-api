package dev.ivfrost.hydro_backend.schedules.internal;

import dev.ivfrost.hydro_backend.config.MqttGateway;
import dev.ivfrost.hydro_backend.devices.DeviceNotFoundException;
import dev.ivfrost.hydro_backend.devices.DeviceResponse;
import dev.ivfrost.hydro_backend.devices.ScheduleDeviceProvider;
import dev.ivfrost.hydro_backend.schedules.ScheduleMapper;
import dev.ivfrost.hydro_backend.devices.ScheduleNotFoundException;
import dev.ivfrost.hydro_backend.schedules.ScheduleRequest;
import dev.ivfrost.hydro_backend.schedules.ScheduleResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Tag(name = "Schedule Service", description = "Service for managing device schedules")
@RequiredArgsConstructor
@Service
public class ScheduleService {

  private final ScheduleRepository scheduleRepository;
  private final ScheduleMapper scheduleMapper;
  private final ConflictService conflictService;
  private final ScheduleDeviceProvider scheduleDeviceProvider;
  private final MqttGateway mqttGateway;
  private final ObjectMapper objectMapper;

  /**
   * Retrieves the schedule for a specific device and day of the week.
   *
   * @param deviceKey the unique key of the device for which the schedule is to be retrieved
   * @param date the date for which the schedule is to be retrieved
   * @return the schedule for the specified device and day of the week
   * @throws DeviceNotFoundException if the device with the given key does not exist
   * @throws ScheduleNotFoundException if the schedule for the given device and day of week does not exist
   */
  public ScheduleResponse getSchedule(String deviceKey, LocalDate date) {
    scheduleDeviceProvider.requireDeviceByKey(deviceKey);
    Schedule schedule = scheduleRepository.findByDeviceKeyAndDate(deviceKey, date)
        .orElseThrow(() -> new ScheduleNotFoundException(deviceKey, date));
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
   * Creates or updates a schedule for a specific device and day of the week and publishes it on
   * the device's command topic.
   * If a schedule already exists for the given device and day, it will be updated with the new
   * time windows provided in the request. If no schedule exists, a new one will be created.
   *
   * @param deviceKey the unique key of the device for which the schedule is to be created or updated
   * @param time the date for which the schedule is to be created or updated
   * @param request the schedule request containing the time windows to be set for the schedule
   * @return the created or updated schedule response
   * @throws DeviceNotFoundException if the device with the given key does not exist
   */
  @Transactional
  public ScheduleResponse upsertSchedule(String deviceKey, LocalDate time, ScheduleRequest request) {
    Schedule schedule = scheduleRepository.findByDeviceKeyAndDate(deviceKey, time)
        .orElseGet(() -> {
          DeviceResponse deviceResponse = scheduleDeviceProvider.getDeviceByKey(deviceKey);
          return new Schedule(null, time, deviceResponse.key(), new ArrayList<>());
        });

    schedule.getWindows().clear();
    schedule.getWindows().addAll(scheduleMapper.toEntity(request.windows()));
    schedule.getWindows().forEach(w -> w.setSchedule(schedule));

    scheduleRepository.save(schedule);
    mqttGateway.sendToMqtt(
        """
        {"action": "SetSchedule", "cause":"Manual", "date": "%s", "windows": %s}
        """.formatted( time, objectMapper.writeValueAsString(request.windows())),
        "hydro/" + deviceKey + "/command"
    );

    return enrich(schedule);
  }


  /**
   * Deletes the schedule for a specific device and day of the week.
   *
   * @param deviceKey the unique key of the device for which the schedule is to be deleted
   * @param date the date for which the schedule is to be deleted
   * @throws DeviceNotFoundException if the device with the given key does not exist
   * @throws ScheduleNotFoundException if the schedule for the given device and day of week
   */
  @Transactional
  public void deleteSchedule(String deviceKey, LocalDate date) {
    Schedule schedule = scheduleRepository.findByDeviceKeyAndDate(deviceKey, date)
        .orElseThrow(() -> new ScheduleNotFoundException(deviceKey, date));
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
    return new ScheduleResponse(response.id(), response.date(), response.windows(), conflictingIds);
  }
}