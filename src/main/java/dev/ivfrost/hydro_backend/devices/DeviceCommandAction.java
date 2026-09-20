package dev.ivfrost.hydro_backend.devices;

import com.fasterxml.jackson.annotation.JsonValue;

public enum DeviceCommandAction {
  START("Start"),
  STOP("Stop"),
  REBOOT("Reboot"),
  GET_STATUS("GetStatus"),
  GET_ALL_STATUS("GetAllStatus"),
  SET_SECRET("SetSecret"),
  SET_TYPE("SetType"),
  SET_NAME("SetName"),
  SET_DESCRIPTION("SetDescription"),
  SET_IMAGE("SetImage"),
  OTA_UPDATE("OtaUpdate"),
  SET_SCHEDULE("SetSchedule");

  public final String label;

  private DeviceCommandAction(String label) {
    this.label = label;
  }

  @JsonValue
  public String getLabel() {
    return label;
  }
}
