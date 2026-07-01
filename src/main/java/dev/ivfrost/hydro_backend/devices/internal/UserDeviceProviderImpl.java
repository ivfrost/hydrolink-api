package dev.ivfrost.hydro_backend.devices.internal;

import dev.ivfrost.hydro_backend.devices.DeviceResponse;
import dev.ivfrost.hydro_backend.devices.DeviceUpdateRequest;
import dev.ivfrost.hydro_backend.devices.UserDeviceProvider;
import java.nio.file.AccessDeniedException;
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
  public DeviceResponse updateUserDevice(long deviceId, DeviceUpdateRequest req, long reqUserId, boolean isAdmin) {
    return deviceService.updateDeviceDetails(deviceId, req, reqUserId, isAdmin);
  }
}
