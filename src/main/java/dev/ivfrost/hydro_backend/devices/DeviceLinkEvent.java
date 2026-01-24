package dev.ivfrost.hydro_backend.devices;

import dev.ivfrost.hydro_backend.config.AmqpConfig;
import org.springframework.modulith.events.Externalized;

@Externalized(target = AmqpConfig.HYDRO_Q)
public record DeviceLinkEvent(DeviceLinkRequest req, Long userId, boolean unlink) {

}
