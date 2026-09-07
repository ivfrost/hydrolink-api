package dev.ivfrost.hydro_backend.devices.internal;

import dev.ivfrost.hydro_backend.devices.DeviceResponse;
import dev.ivfrost.hydro_backend.devices.DeviceUpdateRequest;
import dev.ivfrost.hydro_backend.devices.UserDeviceProvider;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
class UserDeviceProviderImpl implements UserDeviceProvider {

  private final DeviceService deviceService;

  UserDeviceProviderImpl(DeviceService deviceService) {
    this.deviceService = deviceService;
  }

  @Override
  public List<DeviceResponse> getUserDevices(UUID userId) {
    return deviceService.getDevicesByUserId(userId, Pageable.unpaged()).stream().toList();
  }

  @Override
  public DeviceResponse updateUserDevice(String deviceKey, DeviceUpdateRequest req, UUID reqUserId) {
    return deviceService.updateDeviceDetails(deviceKey, req, reqUserId);
  }

  @Override
  public void persistDeviceOrder(UUID reqUserId, List<Long> deviceIds) {
    deviceService.persistDeviceOrder(reqUserId, deviceIds);
  }
}
