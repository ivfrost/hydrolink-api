package dev.ivfrost.hydro_backend.schedules.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScheduleSyncRepository extends JpaRepository<ScheduleSync, String> {

  @Modifying
  @Query(value = """
      INSERT INTO schedule_sync (device_key, version) VALUES (:deviceKey, 1)
      ON CONFLICT (device_key) DO UPDATE SET version = schedule_sync.version + 1
      """, nativeQuery = true)
  void bump(@Param("deviceKey") String deviceKey);

  @Query("select s.version from ScheduleSync s where s.deviceKey = :deviceKey")
  long currentVersion(@Param("deviceKey") String deviceKey);
}