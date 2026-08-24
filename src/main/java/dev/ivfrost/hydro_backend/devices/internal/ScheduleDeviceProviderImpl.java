package dev.ivfrost.hydro_backend.devices.internal;

import dev.ivfrost.hydro_backend.devices.DeviceMapper;
import dev.ivfrost.hydro_backend.devices.DeviceResponse;
import dev.ivfrost.hydro_backend.devices.ScheduleDeviceProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ScheduleDeviceProviderImpl implements ScheduleDeviceProvider {

  private final DeviceMapper deviceMapper;
  private final DeviceService deviceService;

  @Override
  public DeviceResponse getDeviceByKey(String deviceKey) {
    return deviceMapper.deviceToDeviceResponse(deviceService.getDeviceByKey(deviceKey));
  }

  @Override
  public void requireDeviceByKey(String deviceKey) {
    deviceService.requireDeviceByKey(deviceKey);
  }
}
