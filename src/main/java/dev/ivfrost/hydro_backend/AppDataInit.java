package dev.ivfrost.hydro_backend;

import dev.ivfrost.hydro_backend.devices.internal.Device;
import dev.ivfrost.hydro_backend.devices.internal.DeviceRepository;
import dev.ivfrost.hydro_backend.tokens.EncryptionUtil;
import dev.ivfrost.hydro_backend.users.internal.User;
import dev.ivfrost.hydro_backend.users.internal.User.Role;
import dev.ivfrost.hydro_backend.users.internal.UserRepository;
import java.time.Instant;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class AppDataInit implements ApplicationRunner {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final DeviceRepository deviceRepository;
  private final EncryptionUtil encryptionUtil;

  @Value("${seed.admin.email}")
  private String adminEmail;
  @Value("${seed.admin.password}")
  private String adminPassword;
  @Value("${api.mqtt.username}")
  private String apiMqttUsername;
  @Value("${api.mqtt.password}")
  private String apiMqttPassword;
  @Value("${seed.device1.key}")
  private String device1Key;
  @Value("${seed.device1.secret}")
  private String device1Secret;
  @Value("${seed.device2.key}")
  private String device2Key;
  @Value("${seed.device2.secret}")
  private String device2Secret;

  // Seed the database with an admin user and an MQTT API user if they don't exist
  @Override
  public void run(@NonNull ApplicationArguments args) {

    if (userRepository.findByUsername("admin").isEmpty()) {
      userRepository.save(User.builder()
          .username("admin")
          .fullName("Admin User")
          .password(passwordEncoder.encode(adminPassword))
          .email(adminEmail)
          .createdAt(Instant.now())
          .updatedAt(Instant.now())
          .roles(List.of(Role.ADMIN, Role.USER))
          .build());
    }
    if (userRepository.findByUsername(apiMqttUsername).isEmpty()) {
      userRepository.save(User.builder()
          .username(apiMqttUsername)
          .fullName("MQTT API User")
          .password(passwordEncoder.encode(apiMqttPassword))
          .email(String.format("%s@internal.hydro", apiMqttUsername))
          .createdAt(Instant.now())
          .updatedAt(Instant.now())
          .roles(List.of(Role.USER))
          .build());
    }
  }

  // Only on non-prod: Seed the database with two devices if they don't exist
  @Bean
  @Profile("!prod")
  public CommandLineRunner initDevices() {
    return args -> {
      if (deviceRepository.findByKey(device1Key).isEmpty()) {
        deviceRepository.save(Device.builder()
            .key(device1Key)
            .macAddress("00:11:22:33:44:55")
            .firmware("1.0.0")
            .secret(encryptionUtil.encrypt(device1Secret))
            .technicalName("hydro-device-1")
            .friendlyName("Living Room")
            .location("Living Room")
            .build());
      }
      if (deviceRepository.findByKey(device2Key).isEmpty()) {
        deviceRepository.save(Device.builder()
            .key(device2Key)
            .macAddress("66:77:88:99:AA:BB")
            .secret(encryptionUtil.encrypt(device2Secret))
            .firmware("1.0.0")
            .technicalName("hydro-device-2")
            .friendlyName("Kitchen")
            .location("Kitchen")
            .build());
      }
    };
  }
}

