package dev.ivfrost.hydro_backend;

import dev.ivfrost.hydro_backend.devices.internal.Device;
import dev.ivfrost.hydro_backend.devices.internal.DeviceRepository;
import dev.ivfrost.hydro_backend.tokens.EncryptionUtil;
import dev.ivfrost.hydro_backend.users.internal.User;
import dev.ivfrost.hydro_backend.users.internal.User.Role;
import dev.ivfrost.hydro_backend.users.internal.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Profile("dev")
@Component
public class DevDataInit implements ApplicationRunner {
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final DeviceRepository deviceRepository;
  private final EncryptionUtil encryptionUtil;
  @Value("${api.mqtt.username}")
  private String mqttUsername;
  @Value("${api.mqtt.password}")
  private String mqttPassword;
  @Value("${api.mqtt.client.id}")
  private String mqttClientId;

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
    Optional<User> apiUserOpt = userRepository.findByUsername(mqttUsername);
    if (apiUserOpt.isEmpty()) {
      userRepository.save(User.builder()
          .username(mqttUsername)
          .fullName("MQTT API User")
          .password(passwordEncoder.encode(mqttPassword))
          .email(String.format("%s@%s.com", mqttUsername, mqttClientId))
          .createdAt(Instant.now())
          .updatedAt(Instant.now())
          .roles(List.of(
              Role.ADMIN
          ))
          .build());
    }
    if (deviceRepository.count() == 0) {
      deviceRepository.saveAll(List.of(
          Device.builder()
              .key("device1")
              .macAddress("00:11:22:33:44:55")
              .firmware("1.0.0")
              .secret(encryptionUtil.encrypt("secret1"))
              .technicalName("hydro-device-1")
              .friendlyName("Living Room")
              .location("Living Room")
              .build(),
          Device.builder()
              .key("device2")
              .macAddress("66:77:88:99:AA:BB")
              .secret(encryptionUtil.encrypt("secret2"))
              .firmware("1.0.0")
              .technicalName("hydro-device-2")
              .friendlyName("Kitchen")
              .location("Kitchen")
              .build()
      ));
    }
  }
}
