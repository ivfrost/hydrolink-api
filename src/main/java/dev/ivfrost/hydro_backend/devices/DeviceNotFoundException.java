package dev.ivfrost.hydro_backend.devices;

public class DeviceNotFoundException extends RuntimeException {

  public DeviceNotFoundException(String deviceKey) {
    super("Device with key " + deviceKey + " not found.");
  }

}
