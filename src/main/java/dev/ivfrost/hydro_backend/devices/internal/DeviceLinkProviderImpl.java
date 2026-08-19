package dev.ivfrost.hydro_backend.devices.internal;

import dev.ivfrost.hydro_backend.devices.DeviceLinkProvider;
import dev.ivfrost.hydro_backend.devices.DeviceLinkRequest;
import dev.ivfrost.hydro_backend.devices.DeviceResponse;
import dev.ivfrost.hydro_backend.devices.DeviceUnlinkRequest;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class DeviceLinkProviderImpl implements DeviceLinkProvider {

  private final DeviceService deviceService;

  @Override
  public DeviceResponse linkDevice(DeviceLinkRequest req, Long userId) {
   return deviceService.linkDevice(req, userId);
  }

  @Override
  public void unlinkDevice(DeviceUnlinkRequest req, Long userId) {
    deviceService.unlinkDevice(req.deviceKey(), userId);
  }
}
