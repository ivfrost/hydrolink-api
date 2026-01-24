package dev.ivfrost.hydro_backend.devices.internal.adapter;

import dev.ivfrost.hydro_backend.devices.DeviceLinkProvider;
import dev.ivfrost.hydro_backend.devices.DeviceLinkRequest;
import dev.ivfrost.hydro_backend.devices.internal.DeviceService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class DeviceLinkProviderImpl implements DeviceLinkProvider {

  private final DeviceService deviceService;

  @Override
  public void linkDevice(DeviceLinkRequest req, Long userId) {
    deviceService.linkDevice(req, userId, false);
  }

  @Override
  public void unlinkDevice(DeviceLinkRequest req, Long userId) {
    deviceService.linkDevice(req, userId, true);
  }
}
