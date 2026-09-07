package dev.ivfrost.hydro_backend.devices;

import java.util.List;
import java.util.UUID;

public interface UserDeviceProvider {

  List<DeviceResponse> getUserDevices(UUID userId);

  DeviceResponse updateUserDevice(String deviceKey, DeviceUpdateRequest req, UUID reqUserId);

  void persistDeviceOrder(UUID reqUserId, List<Long> deviceIds);
}
