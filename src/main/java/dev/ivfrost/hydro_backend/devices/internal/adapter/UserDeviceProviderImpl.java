package dev.ivfrost.hydro_backend.devices.internal.adapter;

import dev.ivfrost.hydro_backend.devices.DeviceResponse;
import dev.ivfrost.hydro_backend.devices.DeviceUpdateRequest;
import dev.ivfrost.hydro_backend.devices.internal.DeviceService;
import dev.ivfrost.hydro_backend.users.UserDeviceProvider;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class UserDeviceProviderImpl implements UserDeviceProvider {

  private final DeviceService deviceService;

  UserDeviceProviderImpl(DeviceService deviceService) {
    this.deviceService = deviceService;
  }

  @Override
  public List<DeviceResponse> getUserDevices(Long userId) {
    return deviceService.getDevicesByUserId(userId);
  }

  @Override
  public DeviceResponse updateUserDevice(DeviceUpdateRequest req) {
    return deviceService.updateDeviceDetails(req);
  }
}
