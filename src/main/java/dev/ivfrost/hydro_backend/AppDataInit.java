package dev.ivfrost.hydro_backend;

import dev.ivfrost.hydro_backend.config.SeedProperties;
import dev.ivfrost.hydro_backend.devices.DeviceKeyEncryptionUtil;
import dev.ivfrost.hydro_backend.devices.internal.Device;
import dev.ivfrost.hydro_backend.devices.internal.DeviceRepository;
import dev.ivfrost.hydro_backend.users.CognitoUserSyncService;
import dev.ivfrost.hydro_backend.users.internal.User;
import dev.ivfrost.hydro_backend.users.internal.UserRepository;
import dev.ivfrost.hydro_backend.users.internal.UserRole;
import java.time.Instant;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
@Profile("dev")
public class AppDataInit implements ApplicationRunner {

  private static final String ADMIN_USERNAME = "admin";
  private static final String ADMIN_FULL_NAME = "Admin User";

  private final UserRepository userRepository;
  private final DeviceRepository deviceRepository;
  private final DeviceKeyEncryptionUtil encryptionUtil;
  private final CognitoUserSyncService cognitoUserSyncService;
  private final SeedProperties seedProperties;

  // Seed the database with an admin user if it doesn't exist. Identity lives in
  // Cognito now, so the seed creates the Cognito user (dev only) and stores the
  // returned sub on the local row.
  @Override
  public void run(@NonNull ApplicationArguments args) {
    if (userRepository.findByUsername(ADMIN_USERNAME).isPresent()) {
      return;
    }

    String sub = cognitoUserSyncService.ensureDevUser(
        seedProperties.adminEmail(), seedProperties.adminPassword(), ADMIN_USERNAME, ADMIN_FULL_NAME);

    User adminUser = User.builder()
        .sub(sub)
        .username(ADMIN_USERNAME)
        .fullName(ADMIN_FULL_NAME)
        .email(seedProperties.adminEmail())
        .emailVerified(true)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();
    adminUser.getRoles().add(new UserRole(adminUser, UserRole.Role.ADMIN));
    userRepository.save(adminUser);
    log.info("Seeded dev admin user bound to Cognito sub {}", sub);
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
            .secretFingerprint(encryptionUtil.fingerprint(seedProperties.device1Secret()))
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
            .secretFingerprint(encryptionUtil.fingerprint(seedProperties.device2Secret()))
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
