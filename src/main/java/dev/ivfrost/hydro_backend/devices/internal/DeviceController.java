package dev.ivfrost.hydro_backend.devices.internal;

import dev.ivfrost.hydro_backend.ApiResponse;
import dev.ivfrost.hydro_backend.devices.DeviceAuthRequest;
import dev.ivfrost.hydro_backend.devices.DeviceLinkRequest;
import dev.ivfrost.hydro_backend.devices.DeviceOrderSaveRequest;
import dev.ivfrost.hydro_backend.devices.DeviceProvisionRequest;
import dev.ivfrost.hydro_backend.devices.DeviceProvisionResponse;
import dev.ivfrost.hydro_backend.devices.DeviceResponse;
import dev.ivfrost.hydro_backend.devices.DeviceUpdateRequest;
import dev.ivfrost.hydro_backend.tokens.TokenResponse;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Devices Module", description = "API endpoints for device management")
@AllArgsConstructor
@RestController
@RequestMapping("/v1")
public class DeviceController {

  private final DeviceService deviceService;

  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Link device to user by ID (Admin only)",
      description = "Links a device to a specific user by their unique ID using the device's secret as ownership proof.")
  @PostMapping("/users/{userId}/devices/link")
  public ResponseEntity<ApiResponse<Void>> linkDeviceById(
      @RequestBody @Valid DeviceLinkRequest linkDeviceRequest,
      @PathVariable Long userId) {
    deviceService.linkDevice(linkDeviceRequest, userId);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "Device linked to user successfully"));
  }

  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping("/users/{userId}/devices")
  @Operation(summary = "Retrieve devices by user ID (Admin only)",
      description = "Retrieves all devices linked to a specific user by their unique ID.")
  public ResponseEntity<ApiResponse<List<DeviceResponse>>> getUserDevicesById(
      @PathVariable Long userId) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "User devices retrieved successfully",
            deviceService.getDevicesByUserId(userId)));
  }

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

  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Provision new device (Admin only)", description = "Provisions a new device in the system.")
  @PostMapping("/devices")
  public ResponseEntity<ApiResponse<DeviceProvisionResponse>> provisionDevice(
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
            deviceService.updateDeviceDetailsAdmin(deviceId, req)));
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

  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Get device secret by key (Admin only)",
      description = "Retrieves the decrypted device secret for a specific device by its key.")
  @GetMapping("/devices/{deviceKey}/secret")
  public ResponseEntity<ApiResponse<Map<String, String>>> getDeviceSecret(
      @PathVariable String deviceKey) {
    String secret = deviceService.getSecretByDeviceKey(deviceKey);
    Map<String, String> response = Map.of(
        "deviceKey", deviceKey,
        "secret", secret != null ? secret : ""
    );
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "Device secret retrieved successfully", response));
  }

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

  /**
   * Webhook for MQTT broker authentication.
   * This endpoint is called by the MQTT broker to verify the validity of the MQTT token
   * issued to the client.
   */
  @Hidden
  @PostMapping("/internal/mqtt/auth")
  public ResponseEntity<Map<String, Object>> verifyMqttConnection(@RequestBody MqttAuthRequest req) {
    try {
      ResponseEntity<Map<String, Object>> allowedResponse = ResponseEntity.ok(Map.of("result", "allow"));
      if ("hydro-api-user".equals(req.username())) {
        return allowedResponse;
      }
      deviceService.verifyMqttConnection(req);
      return allowedResponse;
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("result", "deny"));
    }
  }

  /**
   * Webhook for MQTT broker ACL authorization.
   * This endpoint is called by the MQTT broker to verify whether a client is authorized
   * to pub/sub to a specific topic.
   * The MQTT token contains the allowed topics for the client, and this endpoint checks
   * whether the requested topic and action (pub/sub) is allowed by the token's claims.
   */
  @Hidden
  @PostMapping("/internal/mqtt/acl")
  public ResponseEntity<Map<String, Object>> verifyMqttAcl(@RequestBody MqttAclRequest req) {
    // Intercept requests from the API and allow it to bypass ACL checks
    if ("hydro-api-user".equals(req.username())) {
      return ResponseEntity.ok(Map.of("result", "allow"));
    }
    try {
      boolean allowed = deviceService.verifyMqttAcl(req);
      if (allowed) {
        return ResponseEntity.ok(Map.of("result", "allow"));
      }
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("result", "deny"));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("result", "deny"));
    }
  }

  @PostMapping("/internal/devices/auth")
    public ResponseEntity<ApiResponse<TokenResponse>> authenticateDevice(@RequestBody DeviceAuthRequest req) {
      return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "Device MQTT auth token retrieved successfully",
            deviceService.authenticateDevice(req)
            ));
    }

  /**
   * Device provisioning endpoint.
   * Expects a Bearer token in the Authorization header for authentication.
   */
  @PostMapping("/internal/devices/provision")
  public ResponseEntity<ApiResponse<DeviceProvisionResponse>> provisionDeviceInternal(
      @RequestHeader ("Authorization") String authorizationHeader,
      @RequestBody @Valid DeviceProvisionRequest req) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(HttpStatus.CREATED, "Device provisioned successfully",
            deviceService.provisionDevice(req, authorizationHeader)));
  }

  public record MqttAuthRequest(String username, String password, String clientid) {}
  public record MqttAclRequest(String username, String clientid, String topic, int action, String password) {}
}

