package dev.ivfrost.hydro_backend.devices.internal;

import io.lettuce.core.dynamic.annotation.Param;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PinRepository extends JpaRepository<Pin, Long> {
  Optional<Pin> findByDeviceAndPinNumber(Device device, Integer pinNumber);
  List<Pin> findByDevice(Device device);
  @Modifying
  @Query("DELETE FROM Pin p WHERE p.device = :device")
  void deleteByDevice(@Param("device") Device device);
}
