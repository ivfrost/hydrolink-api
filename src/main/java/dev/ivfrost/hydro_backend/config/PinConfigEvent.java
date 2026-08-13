package dev.ivfrost.hydro_backend.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PinConfigEvent {

  private final String deviceKey;
  private final String pinsPayload;
}
