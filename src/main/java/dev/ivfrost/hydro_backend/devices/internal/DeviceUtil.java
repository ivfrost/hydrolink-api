package dev.ivfrost.hydro_backend.devices.internal;

import dev.ivfrost.hydro_backend.devices.DeviceResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
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
    return new DeviceResponse(device.getId(), device.getFriendlyName(), device.getLocation(),
        device.getFirmware(),
        device.getTechnicalName(), device.getIp(), device.getCreatedAt(), device.getUpdatedAt(),
        device.getLinkedAt(), device.getLastSeen(), device.getUserId(),
        device.getDisplayOrder());
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

}
