package dev.ivfrost.hydro_backend.devices.internal;

import dev.ivfrost.hydro_backend.ApiResponse;
import dev.ivfrost.hydro_backend.devices.DeviceNotFoundException;
import dev.ivfrost.hydro_backend.devices.PinMapper;
import dev.ivfrost.hydro_backend.devices.PinRequest;
import dev.ivfrost.hydro_backend.devices.PinResponse;
import dev.ivfrost.hydro_backend.devices.PinsNotPersistedException;
import dev.ivfrost.hydro_backend.devices.PinsNotProvidedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Device Pins Module", description = "API endpoints for managing device pins")
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1")
public class PinController {

  private final DeviceRepository deviceRepository;
  private final PinMapper pinMapper;
  private final EntityManager entityManager;
  private final CacheManager cacheManager;

  // TODO: gate endpoints to avoid devices being modified by users who don't own them
  @Operation(summary = "Get all pins for a device", description = "Retrieve all pins associated with a specific device.")
  @GetMapping("/devices/{deviceKey}/pins")
  public ResponseEntity<ApiResponse<List<PinResponse>>> getPins(@PathVariable String deviceKey) {
    Device device = deviceRepository.findByKeyWithPins(deviceKey)
        .orElseThrow(() -> new DeviceNotFoundException(deviceKey));
    Set<Pin> pins = device.getPins();
    if (pins.isEmpty()) {
      throw new PinsNotPersistedException(deviceKey);
    }

    List<PinResponse> response = pins.stream()
        .map(p -> new PinResponse(p.getPinNumber(), p.getMode(), p.getUpdatedAt()))
        .toList();

    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "Pins retrieved successfully", response));
  }

  @Operation(
      summary = "Upsert pins for a device",
      description = "Update existing pins or create new ones for a specific device. Meant to be used"
          + " by the device itself to report its pin configuration."
  )
  @PutMapping("/internal/devices/{deviceKey}/pins")
  @Transactional
  @CacheEvict(value = {"allDevicesCache"}, allEntries = true)
  public ResponseEntity<ApiResponse<Void>> upsertPins(
      @PathVariable String deviceKey,
      @Valid @RequestBody Set<PinRequest> pinRequests) {

    if (pinRequests.isEmpty()) {
      throw new PinsNotProvidedException(deviceKey);
    }

    Device device = deviceRepository.findByKeyWithPins(deviceKey)
        .orElseThrow(() -> new DeviceNotFoundException(deviceKey));

    device.getPins().clear();      // triggers orphanRemoval deletes
    entityManager.flush();         // force DELETEs before INSERTs

    Set<Pin> newPins = pinMapper.pinRequestToPin(pinRequests);
    device.setPins(newPins);

    Cache userCache = cacheManager.getCache("deviceByUserIdCache");
    if (userCache != null) {
      userCache.clear();
    }
    Cache deviceCache = cacheManager.getCache("deviceByKeyCache");
    if (deviceCache != null) {
      deviceCache.evict(deviceKey);
    }

    return ResponseEntity.noContent().build();
  }
}
