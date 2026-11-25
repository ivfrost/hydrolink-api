package dev.ivfrost.hydro_backend.storage.internal;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OtaUpdateRepository extends JpaRepository<OtaUpdate, Long> {

  /**
   * Returns the most recently published OTA update for the given SKU.
   */
  @Query(value = """
      SELECT * FROM ota_updates
      WHERE sku = :sku
      ORDER BY created_at DESC, id DESC
      LIMIT 1
      """, nativeQuery = true)
  Optional<OtaUpdate> findLatestBySku(String sku);

  /**
   * Returns the most recent OTA update for every SKU that has at least one update.
   */
  @Query(value = """
      SELECT DISTINCT ON (sku) *
      FROM ota_updates
      ORDER BY sku, created_at DESC, id DESC
      """, nativeQuery = true)
  List<OtaUpdate> findLatestPerSku();
}
