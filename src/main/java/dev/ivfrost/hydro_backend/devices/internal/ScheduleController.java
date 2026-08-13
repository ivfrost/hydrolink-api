package dev.ivfrost.hydro_backend.devices.internal;

import dev.ivfrost.hydro_backend.ApiResponse;
import dev.ivfrost.hydro_backend.devices.ScheduleRequest;
import dev.ivfrost.hydro_backend.devices.ScheduleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.DayOfWeek;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Device Schedules Module", description = "API endpoints for managing device schedules")
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1")
public class ScheduleController {

  private final ScheduleService scheduleService;

  @Operation(summary = "Get all schedules for a device", description = "Retrieve all schedules associated with a specific device.")
  @GetMapping("/devices/{deviceKey}/schedules")
  public ResponseEntity<ApiResponse<List<ScheduleResponse>>> getSchedules(
      @PathVariable String deviceKey) {
    List<ScheduleResponse> schedules = scheduleService.getSchedules(deviceKey);
    return ResponseEntity.ok(
        ApiResponse.success(HttpStatus.OK, "Schedules retrieved successfully", schedules));
  }

  @Operation(summary = "Get schedule for a specific day", description = "Retrieve the schedule for a specific device on a given day of the week.")
  @GetMapping("/devices/{deviceKey}/schedules/{dayOfWeek}")
  public ResponseEntity<ApiResponse<ScheduleResponse>> getSchedule(
      @PathVariable String deviceKey, @PathVariable DayOfWeek dayOfWeek) {
    ScheduleResponse schedule = scheduleService.getSchedule(deviceKey, dayOfWeek);
    return ResponseEntity.ok(
        ApiResponse.success(HttpStatus.OK, "Schedule retrieved successfully", schedule));
  }

  @Operation(summary = "Update or create schedule for a specific day", description = "Update an existing schedule or create a new one for a specific device on a given day of the week.")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "Schedule details to be updated or created",
      required = true,
      content = @Content(
          schema = @Schema(implementation = ScheduleRequest.class),
          examples = @ExampleObject(
              value = """
                  {
                    "windows": [
                      {
                        "startTime": "08:00",
                        "endTime": "10:00"
                      },
                      {
                        "startTime": "18:00",
                        "endTime": "20:00"
                      }
                    ]
                  }
                  """
          )
      )
  )
  @PutMapping("/devices/{deviceKey}/schedules/{dayOfWeek}")
  public ResponseEntity<ApiResponse<ScheduleResponse>> updateSchedule(
      @PathVariable String deviceKey,
      @PathVariable DayOfWeek dayOfWeek,
      @RequestBody @Valid ScheduleRequest request) {
    ScheduleResponse updated = scheduleService.upsertSchedule(deviceKey, dayOfWeek, request);
    return ResponseEntity.ok(
        ApiResponse.success(HttpStatus.OK, "Schedule updated successfully", updated));
  }

  @Operation(summary = "Delete schedule for a specific day", description = "Delete the schedule for a specific device on a given day of the week.")
  @DeleteMapping("/devices/{deviceKey}/schedules/{dayOfWeek}")
  public ResponseEntity<ApiResponse<Void>> deleteSchedule(
      @PathVariable String deviceKey, @PathVariable DayOfWeek dayOfWeek) {
    scheduleService.deleteSchedule(deviceKey, dayOfWeek);
    return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Schedule deleted", null));
  }
}