package dev.ivfrost.hydro_backend.devices.internal;

import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Slf4j
@Component
public class DeviceSqsIngest {

  private final DeviceStatusHandler handler;

  @SqsListener("${events.queues.device-status-queue}")
  public void receiveStatusMessage(String body) {
    log.info("[{}] Received status message {}", this.getClass().getName(), body);
    handler.onStatusMessage(body);
  }
}
