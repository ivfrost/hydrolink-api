package dev.ivfrost.hydro_backend.devices;

public interface DeviceLinkProvider {

  /**
   * Links a device to a user
   *
   * @param req    the device link request containing the device secret
   * @param userId the user ID to link the device to
   */
  DeviceResponse linkDevice(DeviceLinkRequest req, Long userId);

  /**
   * Unlinks a device from a user
   *
   * @param req    the device link request containing the device secret
   * @param userId the user ID to unlink from
   */
  void unlinkDevice(DeviceUnlinkRequest req, Long userId);
}
