package dev.ivfrost.hydro_backend.tokens;

import dev.ivfrost.hydro_backend.users.UserMqttTokenPayload;

public interface MqttTokenProvider {

  /**
   * Generates an MQTT authentication JWT token for a device
   *
   * @param payload the MQTT token payload containing device ID and topics
   * @return the signed JWT token
   */
  String generateMqttToken(UserMqttTokenPayload payload);
}
