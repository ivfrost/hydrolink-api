package dev.ivfrost.hydro_backend.devices.internal;

import dev.ivfrost.hydro_backend.storage.OtaUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Safety net for firmware updates: re-dispatches the latest persisted OTA update for every
 * technical name that has one, targeting only devices whose firmware is behind.
 *
 * <p>Devices that were offline when a firmware was first published miss the initial MQTT
 * command. This scheduled job re-runs the same {@link DeviceService#dispatchOtaUpdate}
 * logic every 15 minutes so stragglers get notified once they're back online. A fresh
 * presigned URL is minted on each run, so expired URLs never strand a device.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class OtaDispatchScheduler {

  private final DeviceService deviceService;
  private final OtaUpdateService otaUpdateService;

  @Scheduled(fixedDelay = 15 * 60 * 1000L, initialDelay = 30_000L)
  public void redispatchLatestFirmwareUpdates() {
    log.info("Running scheduled OTA re-dispatch sweep");
    var latestUpdates = otaUpdateService.getLatestUpdatesPerTechnicalName();
    if (latestUpdates.isEmpty()) {
      log.debug("No persisted OTA updates to re-dispatch");
      return;
    }
    latestUpdates.forEach(deviceService::dispatchOtaUpdate);
    log.info("Scheduled OTA re-dispatch sweep finished ({} technical names)", latestUpdates.size());
  }
}