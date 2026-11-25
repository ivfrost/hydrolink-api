package dev.ivfrost.hydro_backend.storage.internal;

import dev.ivfrost.hydro_backend.storage.OTAFileUploadEvent;
import dev.ivfrost.hydro_backend.storage.OtaUpdateRecord;
import dev.ivfrost.hydro_backend.storage.OtaUpdateService;
import dev.ivfrost.hydro_backend.storage.UploadResponse;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OtaUpdateServiceImpl implements OtaUpdateService {

  private final OtaUpdateRepository otaUpdateRepository;
  private final StorageService storageService;
  private final ApplicationEventPublisher eventPublisher;

  @Override
  @Transactional
  public void recordFirmwareUpload(
      String sku, String version, boolean forceInstall, UploadResponse upload) {
    OtaUpdate saved = otaUpdateRepository.save(OtaUpdate.builder()
        .version(version)
        .sku(sku)
        .objectKey(upload.objectKey())
        .sha256(upload.sha256())
        .sizeBytes(upload.sizeBytes())
        .forceInstall(forceInstall)
        .build());

    // Publish inside the transaction; @TransactionalEventListener(AFTER_COMMIT)
    // listeners receive it only once the write above has committed.
    eventPublisher.publishEvent(
        new OTAFileUploadEvent(sku, version, forceInstall));

    toRecord(saved);
  }

  @Override
  public Optional<OtaUpdateRecord> getLatestBySku(String sku) {
    return otaUpdateRepository.findLatestBySku(sku)
        .map(this::toRecord);
  }

  @Override
  public List<OtaUpdateRecord> getLatestUpdatesPerSku() {
    return otaUpdateRepository.findLatestPerSku().stream()
        .map(this::toRecord)
        .toList();
  }

  @Override
  public String generatePresignedUrl(String objectKey) {
    return storageService.generatePresignedUrl(objectKey);
  }

  private OtaUpdateRecord toRecord(OtaUpdate update) {
    return new OtaUpdateRecord(
        update.getId(),
        update.getVersion(),
        update.getSku(),
        update.getObjectKey(),
        update.getSha256(),
        update.getSizeBytes(),
        update.isForceInstall(),
        update.getCreatedAt());
  }
}