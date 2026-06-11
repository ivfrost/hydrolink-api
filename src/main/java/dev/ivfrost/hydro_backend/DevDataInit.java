package dev.ivfrost.hydro_backend;

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
  }
}
