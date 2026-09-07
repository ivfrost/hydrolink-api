package dev.ivfrost.hydro_backend.devices.internal;

import dev.ivfrost.hydro_backend.devices.PinConfigEvent;
import dev.ivfrost.hydro_backend.devices.SecretRotatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
public class DeviceStatusHandler {

  private final ObjectMapper objectMapper;
  private final ApplicationEventPublisher eventPublisher;

  public DeviceStatusHandler(ObjectMapper objectMapper, ApplicationEventPublisher eventPublisher) {
    this.objectMapper = objectMapper;
    this.eventPublisher = eventPublisher;
  }

  public void onStatusMessage(String body) {
    JsonNode root = objectMapper.readTree(body);
    String deviceKey = root.path("deviceKey").asString();

    if (root.has("presence")) {
      log.info("Device {} presence: {}", deviceKey, root.path("presence").asString());
      return;
    }

    String event = root.path("event").asString();
    if ("secret_rotated".equals(event)) {
      eventPublisher.publishEvent(new SecretRotatedEvent(deviceKey, body));
    } else if ("pin_config".equals(event)) {
      eventPublisher.publishEvent(new PinConfigEvent(deviceKey, body));
    } else if (root.has("stations")) {
      // ponytail: status is runtime only. Pin modes are set exclusively by the pin_config
      // event, so we do not persist anything here; just note the state arrived.
      log.info("Device {} station status received", deviceKey);
    } else {
      log.warn("Status message without presence, event, or stations for device {}: {}", deviceKey, body);
    }
  }
}
