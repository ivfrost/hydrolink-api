package dev.ivfrost.hydro_backend.storage;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class OTAFileUploadEvent {
  private final String sku;
  private final String firmwareVersion;
  private final boolean forceInstall;
}
