package dev.ivfrost.hydro_backend.devices;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.ImportRuntimeHints;

public record DeviceProvisionRequest(
    @Schema(example = "1.0.0", description = "Firmware version of the device")
    @Size(max = 20)
    @NotBlank
    String firmware,
    @Schema(
        example = "HELTEC-ESP32S3-LORA-V4-16M-8R",
        description = "Hardware SKU, must exist in the device_skus catalog")
    @Size(max = 40)
    @NotBlank
    String sku,
    @Schema(example = "HYDRO-26AX89", description = "Unique key identifying the device, must be exactly 12 characters")
    @Size(max = 12, min = 12)
    String key,
    @Schema(example = "00:1A:2C:3D:4E:5F", description = "MAC address of the device, must be exactly 17 characters")
    @Size(max = 17, min = 17)
    String macAddress) {

}

@ImportRuntimeHints(value = DeviceProvisionRequestRuntimeHints.class)
class DeviceProvisionRequestRuntimeHints implements RuntimeHintsRegistrar {

  @Override
  public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
    hints.reflection().registerType(
        DeviceProvisionRequest.class,
        MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
        MemberCategory.INVOKE_DECLARED_METHODS,
        MemberCategory.ACCESS_DECLARED_FIELDS);
  }
}