package dev.ivfrost.hydro_backend.devices;

import java.util.List;
import java.util.UUID;

public interface DeviceTopicProvider {

  List<String> getTopicsForUser(UUID userId);

}
