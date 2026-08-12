package dev.ivfrost.hydro_backend.storage;

import java.util.List;
import java.util.Optional;

/**
 * Public API of the storage module for querying persisted OTA firmware updates and
 * minting presigned download URLs for stored objects.
 */
public interface OtaUpdateService {

  /**
   * Persists a newly uploaded firmware file and publishes the {@link OTAFileUploadEvent}
   * inside the same transaction. The event is delivered to listeners only after the
   * transaction commits (see {@code AFTER_COMMIT} transactional event listeners), so a
   * failed write never triggers a spurious device notification.
   *
   * @param technicalName technical name of the firmware
   * @param version the published firmware version
   * @param upload the upload result produced by {@link dev.ivfrost.hydro_backend.storage.internal.StorageService}
   * @return the persisted update record
   */
  OtaUpdateRecord recordFirmwareUpload(
      String technicalName, String version, boolean forceInstall, UploadResponse upload);

  /**
   * Returns the most recently published OTA update for the given technical name.
   */
  Optional<OtaUpdateRecord> getLatestByTechnicalName(String technicalName);

  /**
   * Returns the most recent OTA update for each technical name that has at least one
   * published update.
   */
  List<OtaUpdateRecord> getLatestUpdatesPerTechnicalName();

  /**
   * Mints a fresh presigned GET URL for the stored object, rewritten to the externally
   * reachable MinIO endpoint.
   */
  String generatePresignedUrl(String objectKey);
}
