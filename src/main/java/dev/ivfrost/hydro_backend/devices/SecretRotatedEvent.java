package dev.ivfrost.hydro_backend.devices;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class SecretRotatedEvent {
  private final String deviceKey;
  private final String ackPayload;
}