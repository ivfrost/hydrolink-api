package dev.ivfrost.hydro_backend.users.internal;

import jakarta.validation.constraints.Size;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByUsername(String username);

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  boolean existsByUsername(@Size(min = 5, max = 20) String username);
}
