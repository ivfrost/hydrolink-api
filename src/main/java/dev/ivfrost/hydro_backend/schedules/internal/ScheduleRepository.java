package dev.ivfrost.hydro_backend.schedules.internal;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

  List<Schedule> findByDeviceKey(String deviceKey);
  Optional<Schedule> findByDeviceKeyAndDate(String deviceKey, LocalDate date);
}
