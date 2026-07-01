package dev.ivfrost.hydro_backend.devices.internal;

import dev.ivfrost.hydro_backend.devices.DeviceProvisionResponse;
import dev.ivfrost.hydro_backend.devices.DeviceResponse;
import java.util.List;

class DeviceUtil {

  private DeviceUtil() {
  }

  /**
   * Converts a Device entity to a DeviceResponse DTO.
   *
   * @param device the device entity
   * @return the device response DTO
   */
  public static DeviceResponse convertDeviceToResponse(Device device) {
    if (device == null) {
      return null;
    }
    // Extract userId from user object (user is lazily loaded but ID is already known)
    return DeviceResponse.builder()
        .id(device.getId())
        .friendlyName(device.getFriendlyName())
        .location(device.getLocation())
        .firmware(device.getFirmware())
        .technicalName(device.getTechnicalName())
        .ip(device.getIp())
        .createdAt(device.getCreatedAt())
        .updatedAt(device.getUpdatedAt())
        .linkedAt(device.getLinkedAt())
        .lastSeen(device.getLastSeen())
        .userId(device.getUserId())
        .displayOrder(device.getDisplayOrder())
        .build();
  }

  /**
   * Converts a list of Device entities to a list of DeviceResponse DTOs.
   *
   * @param devices the list of device entities
   * @return the list of device response DTOs
   */
  public static List<DeviceResponse> convertDevicesToResponse(List<Device> devices) {
    if (devices == null) {
      return List.of();
    }
    return devices.stream().map(DeviceUtil::convertDeviceToResponse).toList();
  }

  public static DeviceProvisionResponse convertProvisionDeviceToResponse(Device device,
      String rawSecret) {
    if (device == null) {
      return null;
    }
    return new DeviceProvisionResponse(
        convertDeviceToResponse(device),
        rawSecret
    );
  }
}
