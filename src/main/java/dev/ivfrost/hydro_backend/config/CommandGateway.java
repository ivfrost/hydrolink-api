package dev.ivfrost.hydro_backend.config;

public interface CommandGateway {

  /** Sends a command to a single device's /command topic. */
  void publishCommand(String deviceKey, String jsonPayload);

  /** Announces to a device (e.g. an OTA update), optionally marking it retained. */
  void publishAnnounce(String deviceKey, String jsonPayload, boolean retained);

}
