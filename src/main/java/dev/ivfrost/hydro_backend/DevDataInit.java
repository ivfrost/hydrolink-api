package dev.ivfrost.hydro_backend;

import dev.ivfrost.hydro_backend.devices.internal.Device;
import dev.ivfrost.hydro_backend.devices.internal.DeviceRepository;
import dev.ivfrost.hydro_backend.devices.internal.SecretHashUtil;
import dev.ivfrost.hydro_backend.users.internal.User;
import dev.ivfrost.hydro_backend.users.internal.User.Role;
import dev.ivfrost.hydro_backend.users.internal.UserRepository;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class DevDataInit implements ApplicationRunner {
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final DeviceRepository deviceRepository;

  @Override
  public void run(@NonNull ApplicationArguments args) throws Exception {
    if (userRepository.count() == 0) {
      userRepository.save(User.builder()
          .username("admin")
          .fullName("Admin User")
          .password(passwordEncoder.encode("admin"))
          .email("admin@hydro.com")
          .createdAt(Instant.now())
          .updatedAt(Instant.now())
          .roles(List.of(
              Role.ADMIN,
              Role.USER
          ))
          .build());
    }
    if (deviceRepository.count() == 0) {
      deviceRepository.saveAll(List.of(
          Device.builder()
              .key("device1")
              .macAddress("00:11:22:33:44:55")
              .firmware("1.0.0")
              .secretHash(SecretHashUtil.hash("secret1"))
              .technicalName("hydro-device-1")
              .friendlyName("Living Room")
              .location("Living Room")
              .build(),
          Device.builder()
              .key("device2")
              .macAddress("66:77:88:99:AA:BB")
              .secretHash(SecretHashUtil.hash("secret2"))
              .firmware("1.0.0")
              .technicalName("hydro-device-2")
              .friendlyName("Kitchen")
              .location("Kitchen")
              .build()
      ));
    }
  }
}
