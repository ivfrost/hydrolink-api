package dev.ivfrost.hydro_backend.devices;

public class PinsNotPersistedException extends RuntimeException {

  public PinsNotPersistedException(String deviceKey) {
    super("Pins for device with key '" + deviceKey + "' have not been persisted.");
  }
}
