package dev.ivfrost.hydro_backend.repository;

import dev.ivfrost.hydro_backend.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {
    Optional<Device> findByHash(String hash);

    List<Device> findAllByUserId(Long userId);
}
