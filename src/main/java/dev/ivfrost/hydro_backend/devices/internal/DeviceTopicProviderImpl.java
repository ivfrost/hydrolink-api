package dev.ivfrost.hydro_backend.devices.internal;

import dev.ivfrost.hydro_backend.devices.DeviceTopicProvider;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
class DeviceTopicProviderImpl implements DeviceTopicProvider {

  private final DeviceService deviceService;

  public DeviceTopicProviderImpl(DeviceService deviceService) {
    this.deviceService = deviceService;
  }

  @Override
  public List<String> getTopicsForUser(UUID userId) {
    return deviceService.getUserDeviceTopics(userId);
  }
}
