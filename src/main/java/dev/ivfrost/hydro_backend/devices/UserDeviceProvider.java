package dev.ivfrost.hydro_backend.devices;

import java.util.List;

public interface UserDeviceProvider {

  List<DeviceResponse> getUserDevices(Long userId);

  DeviceResponse updateUserDevice(long deviceId, DeviceUpdateRequest request, long reqUserId, boolean isAdmin);
  void persistDeviceOrder(long reqUserId, List<Long> deviceIds);
}
