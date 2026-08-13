package dev.ivfrost.hydro_backend.devices;

import java.time.DayOfWeek;

public class ScheduleNotFoundException extends RuntimeException {

  public ScheduleNotFoundException(String deviceKey, DayOfWeek dayOfWeek) {
    super("Schedule not found for device " + deviceKey + " on " + dayOfWeek);
  }

  public ScheduleNotFoundException(String deviceKey) {
    super("No schedules found for device " + deviceKey);
  }
}
