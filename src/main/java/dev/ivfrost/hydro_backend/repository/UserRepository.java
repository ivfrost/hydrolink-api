package dev.ivfrost.hydro_backend.repository;

import dev.ivfrost.hydro_backend.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.devices WHERE u.id = :id")
    Optional<User> findByIdWithDevices(Long id);

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

}
