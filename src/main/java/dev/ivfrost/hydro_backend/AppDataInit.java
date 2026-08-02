package dev.ivfrost.hydro_backend;

import dev.ivfrost.hydro_backend.config.ApiProperties;
import dev.ivfrost.hydro_backend.config.SeedProperties;
import dev.ivfrost.hydro_backend.devices.internal.Device;
import dev.ivfrost.hydro_backend.devices.internal.DeviceRepository;
import dev.ivfrost.hydro_backend.tokens.DeviceKeyEncriptionUtil;
import dev.ivfrost.hydro_backend.users.internal.User;
import dev.ivfrost.hydro_backend.users.internal.UserRepository;
import dev.ivfrost.hydro_backend.users.internal.UserRole;
import java.time.Instant;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
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
  private final DeviceKeyEncriptionUtil encryptionUtil;
  private final SeedProperties seedProperties;
  private final ApiProperties apiProperties;

  // Seed the database with an admin user and an MQTT API user if they don't exist
  @Override
  public void run(@NonNull ApplicationArguments args) {

    if (userRepository.findByUsername("admin").isEmpty()) {
      User adminUser = User.builder()
          .username("admin")
          .fullName("Admin User")
          .password(passwordEncoder.encode(seedProperties.adminPassword()))
          .email(seedProperties.adminEmail())
          .createdAt(Instant.now())
          .updatedAt(Instant.now())
          .build();
      adminUser.getRoles().add(new UserRole(adminUser, UserRole.Role.ADMIN));
      userRepository.save(adminUser);
    }
    if (userRepository.findByUsername(apiProperties.mqttUsername()).isEmpty()) {
      User mqttApiUser =
      userRepository.save(User.builder()
          .username(apiProperties.mqttUsername())
          .fullName("MQTT API User")
          .password(passwordEncoder.encode(apiProperties.mqttPassword()))
          .email(String.format("%s@internal.hydro", apiProperties.mqttUsername()))
          .createdAt(Instant.now())
          .updatedAt(Instant.now())
          .build());
      mqttApiUser.getRoles().add(new UserRole(mqttApiUser, UserRole.Role.USER));
      userRepository.save(mqttApiUser);
    }
  }

  // Only on non-prod: Seed the database with two devices if they don't exist
  @Bean
  @Profile("!prod")
  public CommandLineRunner initDevices() {
    return args -> {
      if (deviceRepository.findByKey(seedProperties.device1Key()).isEmpty()) {
        deviceRepository.save(Device.builder()
            .key(seedProperties.device1Key())
            .macAddress("00:11:22:33:44:55")
            .firmware("1.0.0")
            .secret(encryptionUtil.encrypt(seedProperties.device1Secret()))
            .technicalName("hydro-device-1")
            .friendlyName("Living Room")
            .locationLabel("Living Room")
            .locationCoordinates("37.7749° N, 122.4194° W")
            .build());
      }
      if (deviceRepository.findByKey(seedProperties.device2Key()).isEmpty()) {
        deviceRepository.save(Device.builder()
            .key(seedProperties.device2Key())
            .macAddress("66:77:88:99:AA:BB")
            .secret(encryptionUtil.encrypt(seedProperties.device2Secret()))
            .firmware("1.0.0")
            .technicalName("hydro-device-2")
            .friendlyName("Kitchen")
            .locationLabel("Kitchen")
            .locationCoordinates("37.7749° N, 122.4194° W")
            .build());
      }
    };
  }
}

