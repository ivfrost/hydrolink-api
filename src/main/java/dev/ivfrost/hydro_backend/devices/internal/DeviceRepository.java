package dev.ivfrost.hydro_backend.devices.internal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeviceRepository extends JpaRepository<Device, Long> {

  @Modifying
  @Query(value = """
    INSERT INTO devices (mac_address, key, firmware, technical_name, secret, secret_fingerprint, created_at, updated_at)
    VALUES (:#{#d.macAddress}, :#{#d.key}, :#{#d.firmware}, :#{#d.technicalName}, :#{#d.secret}, :#{#d.secretFingerprint}, NOW(), NOW())
    ON CONFLICT (mac_address)
    DO UPDATE SET
      key = EXCLUDED.key,
      firmware = EXCLUDED.firmware,
      technical_name = EXCLUDED.technical_name,
      secret = EXCLUDED.secret,
      secret_fingerprint = EXCLUDED.secret_fingerprint,
      updated_at = NOW()
    """, nativeQuery = true)
  void upsert(@Param("d") Device device);

  Page<Device> findAllByUserId(UUID userId, Pageable pageable);

  List<Device> findAllByTechnicalName(String technicalName);

  Optional<Device> findBySecretFingerprint(String secretFingerprint);

  Optional<Device> findByKey(String key);

  @Query("SELECT d FROM Device d LEFT JOIN FETCH d.pins WHERE d.key = :key")
  Optional<Device> findByKeyWithPins(@Param("key") String key);

  @Query("SELECT d FROM Device d LEFT JOIN FETCH d.pins")
  Page<Device> findAllWithPins(Pageable pageable);

  @Query("SELECT d FROM Device d LEFT JOIN FETCH d.pins WHERE d.userId = :userId")
  Page<Device> findAllByUserIdWithPins(@Param("userId") UUID userId, Pageable pageable);

  boolean existsByKeyAndUserSub(String key, String userSub);
}
