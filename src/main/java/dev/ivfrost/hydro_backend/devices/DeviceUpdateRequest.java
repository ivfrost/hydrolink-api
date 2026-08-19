package dev.ivfrost.hydro_backend.devices;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record DeviceUpdateRequest(
    @Schema(
        example = "Front Garden",
        description = "A user-friendly name for the device, used for display purposes."
    )
    @Size(max = 40)
    String friendlyName,
    @Schema(
        example = "hydrolink-core-1",
        description = "The technical identifier for the device."
    )
    @Size(max = 40)
    String technicalName,
    @Schema(
        example = "v1.2.3",
        description = "The firmware version currently installed on the device."
    )
    @Size(max = 20)
    String firmware,
    @Schema(
        example = "Greenhouse 1",
        description = "A label indicating the physical location of the device."
    )
    @Size(max = 255)
    String locationLabel,
    @Schema(
        example = "37.7749,-122.4194",
        description = "The geographical coordinates of the device's location, formatted as 'latitude,longitude'."
    )
    @Size(max = 255)
    String locationCoordinates,
    @Schema(
        example = "Primary hydroponic unit for leafy greens",
        description = "A brief description of the device's purpose or function."
    )
    @Size(max = 255)
    String description,
    @Schema(
        example = "https://hydrolink.io/v1/storage/areas/101/upload",
        description = "A URL pointing to an user uploaded image representing the device"
    )
    @Size(max = 2048)
    String imageUrl,
    @Null // controlled by link/unlink
    Long userId,
    @Positive Integer displayOrder
) {

}
