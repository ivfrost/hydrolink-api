package dev.ivfrost.hydro_backend.devices.internal;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeviceRepository extends JpaRepository<Device, Long> {

  boolean existsByMacAddress(String macAddress);

  @Query(value = """
    INSERT INTO devices (mac_address, key, firmware, technical_name, secret, created_at, updated_at)
    VALUES (:#{#d.macAddress}, :#{#d.key}, :#{#d.firmware}, :#{#d.technicalName}, :#{#d.secret}, NOW(), NOW())
    ON CONFLICT (mac_address)
    DO UPDATE SET
      key = EXCLUDED.key,
      firmware = EXCLUDED.firmware,
      technical_name = EXCLUDED.technical_name,
      secret = EXCLUDED.secret,
      updated_at = NOW()
    RETURNING *;
    """, nativeQuery = true)
  Device upsert(@Param("d") Device device);

  List<Device> findAllByUserId(Long userId);

  Optional<Device> findBySecret(String secret);

  Optional<Device> findByKey(String key);
}
