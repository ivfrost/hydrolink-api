package dev.ivfrost.hydro_backend.devices;

import java.time.DayOfWeek;
import java.time.LocalDate;

public class ScheduleNotFoundException extends RuntimeException {

  public ScheduleNotFoundException(String deviceKey, LocalDate date) {
    super("Schedule not found for device " + deviceKey + " on " + date);
  }

  public ScheduleNotFoundException(String deviceKey) {
    super("No schedules found for device " + deviceKey);
  }
}
