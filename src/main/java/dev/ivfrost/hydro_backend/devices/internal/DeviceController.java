package dev.ivfrost.hydro_backend.devices.internal;

import dev.ivfrost.hydro_backend.ApiResponse;
import dev.ivfrost.hydro_backend.devices.AdminDeviceUpdateRequest;
import dev.ivfrost.hydro_backend.devices.DeviceAuthRequest;
import dev.ivfrost.hydro_backend.devices.DeviceLinkRequest;
import dev.ivfrost.hydro_backend.devices.DeviceProvisionRequest;
import dev.ivfrost.hydro_backend.devices.DeviceProvisionResponse;
import dev.ivfrost.hydro_backend.devices.DeviceResponse;
import dev.ivfrost.hydro_backend.devices.MqttAclRequest;
import dev.ivfrost.hydro_backend.devices.MqttAuthRequest;
import dev.ivfrost.hydro_backend.tokens.TokenResponse;
import dev.ivfrost.hydro_backend.util.PageRequestBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.Map;
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

  // ======= INTERNAL & DEVICE ENDPOINTS =======

  /**
   * Webhook for MQTT broker authentication.
   * This endpoint is called by the MQTT broker to verify the validity of the MQTT token
   * issued to the client.
   */
  @PostMapping("/internal/mqtt/auth")
  public ResponseEntity<Map<String, Object>> verifyMqttConnection(
      @Valid @RequestBody MqttAuthRequest req) {
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
   * Device provisioning endpoint.
   * Expects a Bearer token in the Authorization header for authentication.
   */
  @Operation(
      summary = "Internal device provisioning",
      description = "Provisions a device using an internal Bearer provisioning token."
  )
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = @Content(
          examples = @ExampleObject(
              name = "InternalDeviceProvisionExample",
              value = """
                  {
                    "firmware": "v2.1.0",
                    "technicalName": "hydrolink-core-1",
                    "key": "HYDRO-20AX89",
                    "macAddress": "00:1A:2C:3D:4E:5F"
                  }
                  """,
              summary = "Example of internal device provisioning payload"
          )
      )
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

  /**
   * Webhook for MQTT broker ACL authorization.
   * This endpoint is called by the MQTT broker to verify whether a client is authorized
   * to pub/sub to a specific topic.
   * The MQTT token contains the allowed topics for the client, and this endpoint checks
   * whether the requested topic and action (pub/sub) is allowed by the token's claims.
   */
  @PostMapping("/internal/mqtt/acl")
  public ResponseEntity<Map<String, Object>> verifyMqttAcl(
      @Valid @RequestBody MqttAclRequest req) {
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

  @Operation(
      summary = "Authenticate device",
      description = "Authenticates a hardware device using its credentials and returns an MQTT JWT token."
  )
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = @Content(
          schema = @Schema(implementation = DeviceAuthRequest.class),
          examples = @ExampleObject(
              name = "DeviceAuthExample",
              value = """
                  {
                    "key": "HYDRO-A8JD3F",
                    "secret": "3c375b806be40992a5c199176c498aec"
                  }
                  """,
              summary = "Example of device authentication payload"
          )
      )
  )
  @PostMapping("/internal/devices/auth")
  public ResponseEntity<ApiResponse<TokenResponse>> authenticateDevice(
      @Valid @RequestBody DeviceAuthRequest req) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "Device MQTT auth token retrieved successfully",
            deviceService.authenticateDevice(req)
        ));
  }

  // ======= ADMIN-ONLY ENDPOINTS =======

  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Link device to user by user ID (Admin only)",
      description = "Links a device to a specific user by their unique ID using the device's secret as ownership proof."
  )
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = @Content(
          examples = @ExampleObject(
              name = "DeviceLinkExample",
              value = """
                  {
                    "secret": "e5a0b6840dd5bef62dc8bd1b8431c998"
                  }
                  """,
              summary = "Example of linking a device to a user using secret proof"
          )
      )
  )
  @PostMapping("/users/{userId}/devices/link")
  public ResponseEntity<ApiResponse<Void>> linkDeviceById(
      @Valid @RequestBody DeviceLinkRequest linkDeviceRequest,
      @Parameter(description = "Target user ID", example = "42")
      @PathVariable @Positive Long userId) {
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
      @Parameter(description = "Target user ID", example = "42")
      @PathVariable @Positive Long userId,
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
      summary = "Get all provisioned devices (Admin only)",
      description = "Retrieves all devices provisioned in the system."
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
      summary = "Provision new device (Admin only)",
      description = "Provisions a new device in the system."
  )
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = @Content(
          examples = @ExampleObject(
              name = "AdminDeviceProvisionExample",
              value = """
                  {
                    "serialNumber": "HYDRO-26AX89",
                    "macAddress": "00:1A:2C:3D:4E:5F",
                    "firmwareVersion": "v2.1.0"
                  }
                  """,
              summary = "Example of provisioning a new device via Admin API"
          )
      )
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
    String newSecret = deviceService.regenerateDeviceSecret(deviceKey);
    Map<String, String> response = Map.of(
        "deviceKey", deviceKey,
        "newSecret", newSecret
    );
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "Device secret regenerated successfully", response));
  }
}