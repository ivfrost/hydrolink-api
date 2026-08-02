package dev.ivfrost.hydro_backend.devices;

import java.util.List;

public interface UserDeviceProvider {

  List<DeviceResponse> getUserDevices(Long userId);

  DeviceResponse updateUserDevice(String deviceKey, DeviceUpdateRequest req, long reqUserId);

  void persistDeviceOrder(long reqUserId, List<Long> deviceIds);
}
