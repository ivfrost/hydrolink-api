package dev.ivfrost.hydro_backend.devices;

import java.util.List;

public interface DeviceTopicProvider {

  /**
   * Gets MQTT topics for a user's devices
   *
   * @param userId the user ID
   * @return list of topics in format "hydro/{SECRET}/#"
   */
  List<String> getTopicsForUser(Long userId);
}
