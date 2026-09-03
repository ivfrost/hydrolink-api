package dev.ivfrost.hydro_backend.devices;

public class PinsNotProvidedException extends RuntimeException {

  public PinsNotProvidedException(String deviceKey) {
    super("Pins not provided for device with key: " + deviceKey);
  }
}
