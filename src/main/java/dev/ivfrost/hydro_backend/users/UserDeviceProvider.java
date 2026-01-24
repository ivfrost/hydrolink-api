package dev.ivfrost.hydro_backend.users;

import dev.ivfrost.hydro_backend.devices.DeviceResponse;
import dev.ivfrost.hydro_backend.devices.DeviceUpdateRequest;
import java.util.List;

public interface UserDeviceProvider {

  List<DeviceResponse> getUserDevices(Long userId);

  DeviceResponse updateUserDevice(DeviceUpdateRequest request);
}
