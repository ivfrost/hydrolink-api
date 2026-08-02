package dev.ivfrost.hydro_backend.devices.internal;

import dev.ivfrost.hydro_backend.devices.DeviceResponse;
import dev.ivfrost.hydro_backend.devices.DeviceUpdateRequest;
import dev.ivfrost.hydro_backend.devices.UserDeviceProvider;
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
  public DeviceResponse updateUserDevice(String deviceKey, DeviceUpdateRequest req, long reqUserId) {
    return deviceService.updateDeviceDetails(deviceKey, req, reqUserId);
  }

  @Override
  public void persistDeviceOrder(long reqUserId, List<Long> deviceIds) {
    deviceService.persistDeviceOrder(reqUserId, deviceIds);
  }
}
