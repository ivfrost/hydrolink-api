package dev.ivfrost.hydro_backend.devices;

import dev.ivfrost.hydro_backend.schedules.TimeWindowRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import org.hibernate.validator.constraints.URL;

@Builder
@Schema(
    name = "DeviceCommandRequest",
    description = """
        Command dispatched to a device. Required fields depend on `action`:

        | action            | required fields           |
        |-------------------|---------------------------|
        | SET_SECRET        | secret                    |
        | SET_TYPE          | type                      |
        | SET_NAME          | name                      |
        | SET_DESCRIPTION   | description               |
        | SET_IMAGE         | imageUrl                  |
        | OTA_UPDATE        | binUrl                    |
        | SET_SCHEDULE      | date, windows             |
        | START / STOP      | durationMs                |

        `action` and `cause` are always required.
        """
)
public record DeviceCommandRequest(

    @Schema(
        description = "Command to execute",
        example = "SetSchedule",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    DeviceCommandAction action,

    @Schema(
        description = "Target station on the device. Station ids are zero-based.",
        example = "0",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @PositiveOrZero
    Integer stationId,

    @Schema(
        description = "Why the command was issued",
        example = "Manual",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    DeviceCommandCause cause,

    @Schema(
        description = "Run duration in milliseconds (START/STOP)",
        example = "30000")
    @Positive
    Long durationMs,

    @Schema(
        description = "Station type (SET_TYPE)",
        example = "irrigation")
    @Size(max = 64)
    String type,

    @Schema(
        description = "Station name (SET_NAME)",
        example = "Tomatoes")
    @Size(max = 64)
    String name,

    @Schema(
        description = "Station description (SET_DESCRIPTION)",
        example = "Drip line, north bed")
    @Size(max = 255)
    String description,

    @Schema(
        description = "Device secret (SET_SECRET); 44-char base64",
        example = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
    @Size(min = 44, max = 44)
    String secret,

    @Schema(
        description = "Image URL (SET_IMAGE)",
        example = "https://cdn.example.com/plants/tomato.png")
    @URL
    String imageUrl,

    @Schema(
        description = "Firmware binary URL (OTA_UPDATE)",
        example = "https://cdn.example.com/fw/hydro-1.2.0.bin")
    @URL
    String binUrl,

    @Schema(
        description = "Schedule start date (SET_SCHEDULE)",
        example = "2026-09-16")
    LocalDate date,

    @Schema(description = "Watering windows (SET_SCHEDULE)")
    @Valid
    List<TimeWindowRequest> windows
) {}