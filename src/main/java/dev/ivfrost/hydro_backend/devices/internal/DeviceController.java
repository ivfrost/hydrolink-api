package dev.ivfrost.hydro_backend.devices.internal;

import dev.ivfrost.hydro_backend.ApiResponse;
import dev.ivfrost.hydro_backend.devices.DeviceAuthRequest;
import dev.ivfrost.hydro_backend.devices.DeviceLinkRequest;
import dev.ivfrost.hydro_backend.devices.DeviceProvisionRequest;
import dev.ivfrost.hydro_backend.devices.DeviceResponse;
import dev.ivfrost.hydro_backend.devices.DeviceUpdateRequest;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Devices Module", description = "API endpoints for device management")
@AllArgsConstructor
@RestController
@RequestMapping("/v1")
public class DeviceController {

  private final DeviceService deviceService;

  @Hidden
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Link device to user by ID (Admin only)",
      description = "Links a device to a specific user by their unique ID using the device's secret as ownership proof.")
  @PostMapping("/users/{userId}/devices/link")
  public ResponseEntity<ApiResponse<Void>> linkDeviceById(
      @RequestBody @Valid DeviceLinkRequest linkDeviceRequest,
      @PathVariable Long userId) {
    deviceService.linkDevice(linkDeviceRequest, userId, false);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "Device linked to user successfully"));
  }

  @Hidden
  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping("/users/{userId}/devices")
  @Operation(summary = "Get devices by user ID (Admin only)",
      description = "Retrieves all devices linked to a specific user by their unique ID.")
  public ResponseEntity<ApiResponse<List<DeviceResponse>>> getUserDevicesById(
      @PathVariable Long userId) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "User devices retrieved successfully",
            deviceService.getDevicesByUserId(userId)));
  }

  @Hidden
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Get all provisioned devices (Admin only)",
      description = "Retrieves all devices provisioned in the system.")
  @GetMapping("/devices")
  public ResponseEntity<ApiResponse<Page<DeviceResponse>>> getAllDevices(
      @ParameterObject Pageable pageable) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "All devices retrieved successfully",
            deviceService.getAllDevices(pageable)));
  }

  @Hidden
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Provision new device (Admin only)", description = "Provisions a new device in the system.")
  @PostMapping("/devices")
  public ResponseEntity<ApiResponse<DeviceResponse>> provisionDevice(
      @RequestBody @Valid DeviceProvisionRequest req) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(HttpStatus.CREATED, "Device provisioned successfully",
            deviceService.provisionDevice(req)));
  }

  @Hidden
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Update device by ID (Admin only)",
      description = "Updates the details of a device by its unique ID.")
  @PutMapping("/devices/{deviceId}")
  public ResponseEntity<ApiResponse<DeviceResponse>> updateDeviceDetails
      (@RequestBody @Valid DeviceUpdateRequest req, @PathVariable Long deviceId) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "Device updated successfully",
            deviceService.updateDeviceDetails(req)));
  }

  @Hidden
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Delete device by ID (Admin only)",
      description = "Deletes a device from the system by its unique ID.")
  @DeleteMapping("/devices/{deviceId}")
  public ResponseEntity<ApiResponse<Void>> deleteDeviceById(@PathVariable Long deviceId) {
    deviceService.deleteDeviceById(deviceId);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "Device deleted successfully"));
  }

  @Hidden
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Get device secret by ID",
      description = "Retrieves the decrypted device secret for a specific device by its ID.")
  @GetMapping("/devices/{deviceId}/secret")
  public ResponseEntity<ApiResponse<Map<String, String>>> getDeviceSecret(
      @PathVariable Long deviceId) {
    String secret = deviceService.getSecretByDeviceId(deviceId);
    Map<String, String> response = Map.of(
        "deviceId", deviceId.toString(),
        "secret", secret != null ? secret : ""
    );
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "Device secret retrieved successfully", response));
  }

  @Hidden
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Regenerate device secret (Admin only)",
      description = "Generates a new secret for a device, replacing the old one.")
  @PostMapping("/devices/{deviceId}/secret/regenerate")
  public ResponseEntity<ApiResponse<Map<String, String>>> regenerateDeviceSecret(
      @PathVariable Long deviceId) {
    String newSecret = deviceService.regenerateDeviceSecret(deviceId);
    Map<String, String> response = Map.of(
        "deviceId", deviceId.toString(),
        "newSecret", newSecret
    );
    return ResponseEntity.status(HttpStatus.OK).body(
        ApiResponse.success(HttpStatus.OK, "Device secret regenerated successfully", response));
  }

//  @Operation(summary = "Get device order from Redis for authenticated user",
//      description = "Retrieves the device order stored in Redis for the currently authenticated user.")
//  @GetMapping("/me/devices/order")
//  public ResponseEntity<ApiResponse<DeviceOrderResponse>> getDeviceOrderFromCurrentUser() {
//    Map<String, Long> deviceOrderMap = deviceStateService.getDeviceOrderFromCurrentUser();
//    return ResponseEntity.status(HttpStatus.OK)
//        .body(ApiResponse.success(HttpStatus.OK, "Device order retrieved successfully",
//            new DeviceOrderResponse(deviceOrderMap)));
//  }

  @Operation(summary = "Update device friendly friendlyName",
      description = "Updates the friendly friendlyName of a device linked to the currently authenticated user.")
  @PutMapping("/me/devices/{deviceId}")
  public ResponseEntity<ApiResponse<DeviceResponse>> updateDeviceNickname(
      @RequestBody @Valid DeviceUpdateRequest req) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "Device nickname updated successfully",
            deviceService.updateDeviceFriendlyName(req)));
  }

  @Operation(summary = "Authenticate device for MQTT",
      description = "Authenticates a device and returns a signed JWT token for MQTT broker authentication. The device uses its ID and secret as credentials.")
  @PostMapping("/devices/auth/mqtt")
  public ResponseEntity<ApiResponse<Map<String, String>>> authenticateDeviceForMqtt(
      @RequestBody @Valid DeviceAuthRequest req) {
    String mqttToken = deviceService.getMqttAuthToken(req);
    Map<String, String> response = Map.of(
        "deviceId", req.deviceId().toString(),
        "token", mqttToken
    );
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "Device MQTT token generated successfully",
            response));
  }
}
