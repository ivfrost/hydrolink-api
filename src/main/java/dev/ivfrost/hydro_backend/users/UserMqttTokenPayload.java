package dev.ivfrost.hydro_backend.users;

import java.util.List;

public record UserMqttTokenPayload(Long userId, List<String> topics) {

}

