  package dev.ivfrost.hydro_backend.devices.internal;

  import com.fasterxml.jackson.core.JsonProcessingException;
  import com.fasterxml.jackson.databind.JsonNode;
  import com.fasterxml.jackson.databind.ObjectMapper;
  import dev.ivfrost.hydro_backend.config.PinConfigEvent;
  import dev.ivfrost.hydro_backend.devices.PinMode;
  import jakarta.transaction.Transactional;
  import lombok.RequiredArgsConstructor;
  import org.springframework.context.event.EventListener;
  import org.springframework.stereotype.Service;

  @RequiredArgsConstructor
  @Service
  public class PinConfigService {

    private final DeviceRepository deviceRepository;
    private final PinRepository pinRepository;
    private final ObjectMapper objectMapper;

    public void handlePinConfig(String deviceKey, JsonNode pinsArray) {
      Device device = deviceRepository.findByKey(deviceKey)
          .orElseThrow(() -> new IllegalStateException("Unknown device: " + deviceKey));

      for (JsonNode pinNode : pinsArray) {
        int pinNumber = pinNode.path("pin").asInt();
        PinMode mode = PinMode.valueOf(pinNode.path("mode").asText());

        Pin pin = pinRepository.findByDeviceAndPinNumber(device, pinNumber)
            .orElseGet(() -> {
              Pin p = new Pin();
              p.setDevice(device);
              p.setPinNumber(pinNumber);
              return p;
            });
        pin.setMode(mode);
        pinRepository.save(pin);
      }
    }

    @Transactional
    @EventListener
    public void handlePinConfigEvent(PinConfigEvent event) throws JsonProcessingException {
      JsonNode root = objectMapper.readTree(event.getPinsPayload());
      JsonNode pinsArray = root.path("pins");
      handlePinConfig(event.getDeviceKey(), pinsArray);
    }
  }
