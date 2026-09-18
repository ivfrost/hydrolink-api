package dev.ivfrost.hydro_backend.devices;

import com.fasterxml.jackson.annotation.JsonValue;

public enum DeviceCommandCause {
  MANUAL("Manual"),
  SENSOR("Sensor"),
  SCHEDULE("Schedule"),
  DONE("Done");

  public final String label;

  private DeviceCommandCause(String label) {
    this.label = label;
  }

  @JsonValue
  public String getLabel() {
    return label;
  }
}
