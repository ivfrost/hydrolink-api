package dev.ivfrost.hydro_backend.devices.internal;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRepository extends JpaRepository<Device, Long> {

  boolean existsByMacAddress(String macAddress);


  List<Device> findAllByUserId(Long userId);

  Optional<Device> findBySecret(String secret);

  Optional<Device> findByKey(String key);
}
