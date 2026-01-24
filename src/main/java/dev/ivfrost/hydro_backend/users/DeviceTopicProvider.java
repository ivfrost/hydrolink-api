package dev.ivfrost.hydro_backend.users;

import java.util.List;

public interface DeviceTopicProvider {

  List<String> getTopicsForUser(Long userId);

}
