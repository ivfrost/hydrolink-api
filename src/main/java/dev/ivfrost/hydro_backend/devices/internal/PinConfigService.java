package dev.ivfrost.hydro_backend.devices.internal;

import dev.ivfrost.hydro_backend.devices.PinMapper;
import dev.ivfrost.hydro_backend.devices.PinResponse;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import dev.ivfrost.hydro_backend.config.PinConfigEvent;
import dev.ivfrost.hydro_backend.devices.PinMode;

@RequiredArgsConstructor
@Slf4j
@Service
public class PinConfigService {

  private final DeviceRepository deviceRepository;
  private final PinRepository pinRepository;
  private final ObjectMapper objectMapper;
  private final EntityManager entityManager;
  private final PinMapper pinMapper;

  public void handlePinConfig(String deviceKey, JsonNode pinsArray) {
    Device device = deviceRepository.findByKey(deviceKey)
        .orElseThrow(() -> new IllegalStateException("Unknown device: " + deviceKey));

    for (JsonNode pinNode : pinsArray) {
      int pinNumber = pinNode.path("pin").asInt();
      PinMode mode = "Output".equalsIgnoreCase(pinNode.path("mode").asString())
          ? PinMode.OUTPUT
          : PinMode.INPUT;

      Pin pin = pinRepository.findByDeviceAndPinNumber(device, pinNumber)
          .orElseGet(() -> {
            Pin p = new Pin();
            p.setDevice(device);
            p.setPinNumber(pinNumber);
            return p;
          });
      pin.setMode(mode);
      log.debug("Saving pin config for device {}: pin {}, mode {}", deviceKey, pinNumber, mode);
      pinRepository.save(pin);
    }
  }

  @Transactional
  @EventListener
  public void handlePinConfigEvent(PinConfigEvent event) {
    String payload = event.getPinsPayload();
    JsonNode root = objectMapper.readTree(payload);
    JsonNode pinsArray = root.path("pins");
    handlePinConfig(event.getDeviceKey(), pinsArray);
  }

  @Caching(evict = {
      @CacheEvict(value = "deviceByKeyCache", key = "#deviceKey"),
      @CacheEvict(value = "deviceByUserIdCache", allEntries = true),
      @CacheEvict(value = "allDevicesCache", allEntries = true)
  })
  @Transactional
  public void replacePinsForDevice(String deviceKey, List<PinResponse> pinsResponse) {
    Device device = deviceRepository.findByKey(deviceKey)
        .orElseThrow(() -> new IllegalStateException("Device not found: " + deviceKey));

    // Delete existing pins now
    pinRepository.deleteByDevice(device);
    entityManager.flush();

    List<Pin> pins = pinMapper.pinResponseToPin(pinsResponse);

    // Set device reference and save new pins
    for (Pin pin : pins) {
      pin.setDevice(device);
      pin.setUpdatedAt(Instant.now());
    }
    pinRepository.saveAll(pins);
  }
}