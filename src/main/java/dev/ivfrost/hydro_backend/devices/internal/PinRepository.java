package dev.ivfrost.hydro_backend.devices.internal;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PinRepository extends JpaRepository<Pin, Long> {
  Optional<Pin> findByDeviceAndPinNumber(Device device, Integer pinNumber);
  List<Pin> findByDevice(Device device);

}
