package dev.ivfrost.hydro_backend.users;

import dev.ivfrost.hydro_backend.config.AmqpConfig;
import org.springframework.modulith.events.Externalized;

@Externalized(target = AmqpConfig.HYDRO_Q)
public record UserUpdateLastOnlineEvent(String username) {

}
