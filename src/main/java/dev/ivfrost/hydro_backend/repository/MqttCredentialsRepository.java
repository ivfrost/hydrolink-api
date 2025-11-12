package dev.ivfrost.hydro_backend.repository;

import dev.ivfrost.hydro_backend.entity.MqttCredentials;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MqttCredentialsRepository extends JpaRepository<MqttCredentials, Long> {

    boolean existsByUserId(Long userId);
    Optional<MqttCredentials> findByUserId(Long userId);
}
