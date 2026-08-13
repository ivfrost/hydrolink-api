package dev.ivfrost.hydro_backend.devices.internal;

import dev.ivfrost.hydro_backend.ApiResponse;
import dev.ivfrost.hydro_backend.devices.DeviceNotFoundException;
import dev.ivfrost.hydro_backend.devices.PinResponse;
import dev.ivfrost.hydro_backend.devices.PinsNotPersistedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Device Pins Module", description = "API endpoints for managing device pins")
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1")
public class PinController {

  private final PinRepository pinRepository;
  private final DeviceRepository deviceRepository;

  @Operation(summary = "Get all pins for a device", description = "Retrieve all pins associated with a specific device.")
  @GetMapping("/devices/{deviceKey}/pins")
  public ResponseEntity<ApiResponse<List<PinResponse>>> getPins(@PathVariable String deviceKey) {
    Device device = deviceRepository.findByKey(deviceKey)
        .orElseThrow(() -> new DeviceNotFoundException(deviceKey));

    List<Pin> pins = pinRepository.findByDevice(device);
    if (pins.isEmpty()) {
      throw new PinsNotPersistedException(deviceKey);
    }

    List<PinResponse> response = pins.stream()
        .map(p -> new PinResponse(p.getPinNumber(), p.getMode(), p.getLabel()))
        .toList();

    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "Pins retrieved successfully", response));
  }
}
