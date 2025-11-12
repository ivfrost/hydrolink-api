package dev.ivfrost.hydro_backend.controller;

import dev.ivfrost.hydro_backend.dto.*;
import dev.ivfrost.hydro_backend.service.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Device Management", description = "API endpoints for managing devices")
@AllArgsConstructor
@RestController
@RequestMapping("/v1")
public class DeviceController {

    private final DeviceService deviceService;


    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Link device to user by ID (Admin only)",
        description = "Links a device to a specific user by their unique ID using the device's ownership hash. Access restricted to administrators."
    )
    @PostMapping("/users/{userId}/devices/link")
    public ResponseEntity<ApiResponse<Void>> linkDeviceById(
            @RequestBody @Valid DeviceLinkRequest linkDeviceRequest,
            @PathVariable Long userId) {

        deviceService.linkDevice(linkDeviceRequest, userId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.build(HttpStatus.OK, "Device linked to user successfully", null));
    }

    @Operation(
            summary = "Unlink device from authenticated user",
            description = "Unlinks a device from the currently authenticated user using the device's unique ID."
    )
    @DeleteMapping("/me/devices/{deviceId}/unlink")
    public ResponseEntity<ApiResponse<Void>> unlinkDevice(@PathVariable Long deviceId) {
        deviceService.unlinkDevice(deviceId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.build(HttpStatus.OK, "Device unlinked from user successfully", null));
    }

    @Operation(
        summary = "Get linked devices",
        description = "Retrieves all devices linked to the currently authenticated user."
    )
    @GetMapping("/me/devices")
    public ResponseEntity<ApiResponse<List<DeviceResponse>>> getUserDevices() {
        List<DeviceResponse> response = deviceService.getUserDevices();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.build(HttpStatus.OK, "Devices retrieved successfully", response));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/{userId}/devices")
    @Operation(
            summary = "Get devices by user ID (Admin only)",
            description = "Retrieves all devices linked to a specific user by their unique ID."
    )
    public ResponseEntity<ApiResponse<List<DeviceResponse>>> getUserDevicesById(@PathVariable Long userId) {
        List<DeviceResponse> response = deviceService.getUserDevicesById(userId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.build(HttpStatus.OK, "Devices retrieved successfully", response));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get all provisioned devices (Admin only)",
            description = "Retrieves all devices provisioned in the system."
    )
    @GetMapping("/devices")
    public ResponseEntity<ApiResponse<List<DeviceResponse>>> getAllDevices() {
        List<DeviceResponse> response = deviceService.getAllDevices();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.build(HttpStatus.OK, "All devices retrieved successfully", response));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Provision new device (Admin only)",
        description = "Provisions a new device in the system."
    )
    @PostMapping("/devices")
    public ResponseEntity<ApiResponse<DeviceResponse>> provisionDevice(@RequestBody @Valid DeviceProvisionRequest req) {
        DeviceResponse device = deviceService.provisionDevice(req);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.build(HttpStatus.CREATED, "Device provisioned successfully", device));
    }

    @Operation(
            summary = "Update order or user-defined name of a device",
            description = "Updates the display order or user-defined name of a device linked to the authenticated user."
    )
    @PutMapping("/me/devices/{deviceId}")
    public ResponseEntity<ApiResponse<DeviceResponse>> updateUserDeviceById(
            @PathVariable Long deviceId,
            @RequestBody @Valid DeviceUpdateRequest req) {

        DeviceResponse updatedDevice = deviceService.updateUserDeviceById(deviceId, req);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.build(HttpStatus.OK, "Device updated successfully", updatedDevice));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Update device by ID (Admin only)",
        description = "Updates the details of a device by its unique ID."
    )
    @PutMapping("/devices/{deviceId}")
    public ResponseEntity<ApiResponse<DeviceResponse>> updateDeviceById(
            @PathVariable Long deviceId,
            @RequestBody @Valid DeviceUpdateRequest req,
            @RequestParam String technicalName,
            @RequestParam String firmware) {

        DeviceResponse updatedDevice = deviceService.updateDeviceById(deviceId, req, technicalName, firmware);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.build(HttpStatus.OK, "Device updated successfully", updatedDevice));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Delete device by ID (Admin only)",
        description = "Deletes a device from the system by its unique ID."
    )
    @DeleteMapping("/devices/{deviceId}")
    public ResponseEntity<ApiResponse<Void>> deleteDeviceById(@PathVariable Long deviceId) {
        deviceService.deleteDeviceById(deviceId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.build(HttpStatus.OK, "Device deleted successfully", null));
    }
}
