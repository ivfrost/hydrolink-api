package dev.ivfrost.hydro_backend.config;

import dev.ivfrost.hydro_backend.devices.DeviceCommandRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient;
import software.amazon.awssdk.services.iotdataplane.model.PublishRequest;
import tools.jackson.databind.ObjectMapper;

/**
 * Command seam backed by the AWS IoT Data Plane Publish API (IAM-signed HTTP).
 *
 * <p>The backend holds no broker session and no certificate, it only signs short-lived publish
 * calls.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AwsCommandGateway implements CommandGateway {

  private static final String TOPIC_PREFIX = "hydro/";

  private final IotDataPlaneClient iotData;
  private final ObjectMapper objectMapper;

  @Override
  public void publishCommand(String deviceKey, DeviceCommandRequest commandRequest) {
    String jsonPayload = objectMapper.writeValueAsString(commandRequest);
    publish(deviceKey, "command", jsonPayload, false);
  }

  @Override
  public void publishAnnounce(String deviceKey, String jsonPayload, boolean retained) {
    publish(deviceKey, "announce", jsonPayload, retained);
  }

  private void publish(String deviceKey, String leaf, String jsonPayload, boolean retained) {
    String topic = TOPIC_PREFIX + deviceKey + "/" + leaf;
    iotData.publish(PublishRequest.builder()
        .topic(topic)
        .qos(1)
        .retain(retained)
        .payload(SdkBytes.fromUtf8String(jsonPayload))
        .build());
    log.debug("Published to {} retaining={}: {}", topic, retained, jsonPayload);
  }
}