package dev.ivfrost.hydro_backend;

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
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Profile("!dev")
@RequiredArgsConstructor
@Component
public class AppDataInit implements ApplicationRunner {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Value("${seed.admin.email}")
  private String adminEmail;
  @Value("${seed.admin.password}")
  private String adminPassword;
  @Value("${mqtt.username}")
  private String apiMqttUsername;
  @Value("${mqtt.password}")
  private String apiMqttPassword;

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
}