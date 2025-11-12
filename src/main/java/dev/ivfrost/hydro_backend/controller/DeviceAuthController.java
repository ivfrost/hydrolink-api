package dev.ivfrost.hydro_backend.controller;

import dev.ivfrost.hydro_backend.dto.ApiResponse;
import dev.ivfrost.hydro_backend.dto.DeviceLinkRequest;
import dev.ivfrost.hydro_backend.dto.MqttCredentialsResponse;
import dev.ivfrost.hydro_backend.service.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@Tag(name = "Device Authentication", description = "API endpoints for proving device ownership and getting credentials")
@RestController
@RequestMapping("/v1")
public class DeviceAuthController {

    private final DeviceService deviceService;

    @Operation(
            summary = "Link device to authenticated user",
            description = "Links a device to the currently authenticated user using the device's ownership hash."
    )
    @PostMapping("/me/devices/link")
    public ResponseEntity<ApiResponse<Void>> linkDevice(@RequestParam String hash) {
        deviceService.linkDevice(new DeviceLinkRequest(hash));
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.build(HttpStatus.OK, "Device linked to user successfully", null));
    }

    @Operation(
            summary = "Get MQTT credentials",
            description = "Retrieves MQTT credentials for the currently authenticated user."
    )
    @GetMapping("/me/devices/credentials")
    public ResponseEntity<ApiResponse<MqttCredentialsResponse>> getMqttCredentials() {
        MqttCredentialsResponse credentials = deviceService.getMqttCredentials();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.build(HttpStatus.OK, "MQTT credentials retrieved successfully", credentials));
    }
}
