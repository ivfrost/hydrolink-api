package dev.ivfrost.hydro_backend.storage.internal;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OtaUpdateRepository extends JpaRepository<OtaUpdate, Long> {

  /**
   * Returns the most recently published OTA update for the given technical name.
   */
  @Query(value = """
      SELECT * FROM ota_updates
      WHERE technical_name = :technicalName
      ORDER BY created_at DESC, id DESC
      LIMIT 1
      """, nativeQuery = true)
  Optional<OtaUpdate> findLatestByTechnicalName(String technicalName);

  /**
   * Returns the most recent OTA update for every technical name that has at least one update.
   */
  @Query(value = """
      SELECT DISTINCT ON (technical_name) *
      FROM ota_updates
      ORDER BY technical_name, created_at DESC, id DESC
      """, nativeQuery = true)
  List<OtaUpdate> findLatestPerTechnicalName();
}
