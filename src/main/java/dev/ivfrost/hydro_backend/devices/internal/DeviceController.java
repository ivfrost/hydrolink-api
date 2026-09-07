package dev.ivfrost.hydro_backend.devices.internal;

import dev.ivfrost.hydro_backend.ApiResponse;
import dev.ivfrost.hydro_backend.devices.AdminDeviceUpdateRequest;
import dev.ivfrost.hydro_backend.devices.DeviceLinkRequest;
import dev.ivfrost.hydro_backend.devices.DeviceProvisionRequest;
import dev.ivfrost.hydro_backend.devices.DeviceProvisionResponse;
import dev.ivfrost.hydro_backend.devices.DeviceResponse;
import dev.ivfrost.hydro_backend.util.PageRequestBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Devices Module", description = "API endpoints for device management")
@AllArgsConstructor
@RestController
@Validated
@RequestMapping("/v1")
public class DeviceController {

  private final DeviceService deviceService;
  private static final Map<String, Object> ALLOW_ACL_MAP = Map.of("result", "allow");
  private static final Map<String, Object> DENY_ACL_MAP = Map.of("result", "deny");

  // ======= INTERNAL & DEVICE ENDPOINTS =======

  /**
   * Registers a device record for a booting ESP32 and issues its ownership secret.
   * Expects a Bearer token in the Authorization header for authentication.
   */
  @Operation(
      summary = "Register device record (ESP boot)",
      description = "Registers the application device record and issues an ownership secret. POST, not GET, so a proxy never caches it. Separate from the AWS Thing, which the ESP provisions itself through fleet provisioning on first boot."
  )
  @PostMapping("/internal/devices/provision")
  public ResponseEntity<ApiResponse<DeviceProvisionResponse>> provisionDeviceInternal(
      @Parameter(description = "Internal bearer provisioning token", example = "Bearer prov_tok_123456789")
      @RequestHeader("Authorization") @NotBlank String authorizationHeader,
      @Valid @RequestBody DeviceProvisionRequest req) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(HttpStatus.CREATED, "Device provisioned successfully",
            deviceService.provisionDevice(req, authorizationHeader)));
  }

  // ======= ADMIN-ONLY ENDPOINTS =======

  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Link device to user by user ID (Admin only)",
      description = "Links a device to a specific user by their unique ID using the device's secret as ownership proof."
  )
  @PostMapping("/users/{userId}/devices/link")
  public ResponseEntity<ApiResponse<Void>> linkDeviceById(
      @Valid @RequestBody DeviceLinkRequest linkDeviceRequest,
      @Parameter(description = "Target user ID")
      @PathVariable UUID userId) {
    deviceService.linkDevice(linkDeviceRequest, userId);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "Device linked to user successfully"));
  }

  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Retrieve devices by user ID (Admin only)",
      description = "Retrieves all devices linked to a specific user by their unique ID."
  )
  @GetMapping("/users/{userId}/devices")
  public ResponseEntity<ApiResponse<Page<DeviceResponse>>> getUserDevicesById(
      @Parameter(description = "Target user ID")
      @PathVariable UUID userId,
      @Parameter(description = "Page number for pagination (1-based index)", example = "1")
      @RequestParam(required = false) Integer page,
      @Parameter(description = "Number of devices per page", example = "10")
      @RequestParam(required = false) Integer size
      ) {
    Pageable pageable = PageRequestBuilder.buildPageRequest(page, size, "createdAt", Direction.DESC);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "User devices retrieved successfully",
            deviceService.getDevicesByUserId(userId, pageable)));
  }

  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Get all device records (Admin only)",
      description = "Retrieves all device records known to the application (the app-side rows; not the AWS Things, which the devices register through AWS fleet provisioning)."
  )
  @GetMapping("/devices")
  public ResponseEntity<ApiResponse<Page<DeviceResponse>>> getAllDevices(
      @Parameter(description = "Page number for pagination (1-based index)", example = "1")
      @RequestParam(required = false) Integer page,
      @Parameter(description = "Number of devices per page", example = "10")
      @RequestParam(required = false) Integer size) {
    Pageable pageable = PageRequestBuilder.buildPageRequest(page, size, "createdAt", Direction.DESC);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "All devices retrieved successfully",
            deviceService.getAllDevices(pageable)));
  }

  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Register device record (Admin only)",
      description = "Registers an application device record and issues an ownership secret. POST, not GET, so a proxy never caches it. Separate from the AWS Thing, which the ESP provisions itself through fleet provisioning on first boot."
  )
  @PostMapping("/devices")
  public ResponseEntity<ApiResponse<DeviceProvisionResponse>> provisionDevice(
      @Valid @RequestBody DeviceProvisionRequest req) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(HttpStatus.CREATED, "Device provisioned successfully",
            deviceService.provisionDevice(req)));
  }

  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Update device by key (Admin only)",
      description = "Updates the details of a device by its unique key."
  )
  @PutMapping("/devices/{deviceKey}")
  public ResponseEntity<ApiResponse<DeviceResponse>> updateDeviceDetails(
      @Valid @RequestBody AdminDeviceUpdateRequest req,
      @Parameter(description = "Target device key", example = "HYDRO-A8JD3F")
      @PathVariable String deviceKey) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "Device updated successfully",
            deviceService.updateDeviceDetailsAdmin(deviceKey, req)));
  }

  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Delete device by key (Admin only)",
      description = "Deletes a device from the system by its unique key."
  )
  @DeleteMapping("/devices/{deviceKey}")
  public ResponseEntity<ApiResponse<Void>> deleteDeviceById(
      @Parameter(description = "Target device key", example = "HYDRO-A8JD3F")
      @PathVariable String deviceKey) {
    deviceService.deleteDeviceByKey(deviceKey);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "Device deleted successfully"));
  }

  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Get device secret by key (Admin only)",
      description = "Retrieves the decrypted device secret for a specific device by its key. "
          + "Uses POST because exposing credential material through a GET URL makes it more "
          + "likely to be recorded in proxy/access logs."
  )
  @PostMapping("/devices/{deviceKey}/secret")
  public ResponseEntity<ApiResponse<Map<String, String>>> getDeviceSecret(
      @Parameter(description = "Unique device key", example = "HYDRO-AL343K")
      @PathVariable @NotBlank String deviceKey) {
    String secret = deviceService.getSecretByDeviceKey(deviceKey);
    Map<String, String> response = Map.of(
        "deviceKey", deviceKey,
        "secret", secret != null ? secret : ""
    );
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "Device secret retrieved successfully", response));
  }

  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Regenerate device secret (Admin only)",
      description = "Generates a new secret for a device, replacing the old one."
  )
  @PostMapping("/devices/{deviceKey}/secret/regenerate")
  public ResponseEntity<ApiResponse<Map<String, String>>> regenerateDeviceSecret(
      @Parameter(description = "Target device key", example = "HYDRO-A8JD3F")
      @PathVariable String deviceKey) {
    String newSecret = deviceService.regenerateDeviceSecret(deviceKey, true);
    Map<String, String> response = Map.of(
        "deviceKey", deviceKey,
        "newSecret", newSecret
    );
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "Device secret regenerated successfully", response));
  }

  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Regenerate device secret with no ack (Admin only)",
      description = "Generates a new secret for a device, replacing the old one. Saves the new secret to database without device ack."
  )
  @PostMapping("/devices/{deviceKey}/secret/regenerate/no-ack")
  public ResponseEntity<ApiResponse<Map<String, String>>> regenerateDeviceSecretNoAck(
      @Parameter(description = "Target device key", example = "HYDRO-A8JD3F")
      @PathVariable String deviceKey) {
    String newSecret = deviceService.regenerateDeviceSecret(deviceKey, false);
    Map<String, String> response = Map.of(
        "deviceKey", deviceKey,
        "newSecret", newSecret
    );
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "Device secret regenerated successfully (no ack)", response));
  }
}