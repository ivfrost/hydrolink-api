package dev.ivfrost.hydro_backend.devices;

import java.util.List;

public interface UserDeviceProvider {

  List<DeviceResponse> getUserDevices(Long userId);

  DeviceResponse updateUserDevice(long deviceId, DeviceUpdateRequest request, long reqUserId);
  DeviceResponse updateUserDeviceAdmin(long deviceId, AdminDeviceUpdateRequest request, long reqUserId);
  void persistDeviceOrder(long reqUserId, List<Long> deviceIds);
}
