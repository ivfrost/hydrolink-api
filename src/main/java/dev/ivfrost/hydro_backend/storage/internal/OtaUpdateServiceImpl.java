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
  public OtaUpdateRecord recordFirmwareUpload(
      String technicalName, String version, boolean forceInstall, UploadResponse upload) {
    OtaUpdate saved = otaUpdateRepository.save(OtaUpdate.builder()
        .version(version)
        .technicalName(technicalName)
        .objectKey(upload.objectKey())
        .sha256(upload.sha256())
        .sizeBytes(upload.sizeBytes())
        .forceInstall(forceInstall)
        .build());

    // Publish inside the transaction; @TransactionalEventListener(AFTER_COMMIT)
    // listeners receive it only once the write above has committed.
    eventPublisher.publishEvent(
        new OTAFileUploadEvent(technicalName, version, forceInstall));

    return toRecord(saved);
  }

  @Override
  public Optional<OtaUpdateRecord> getLatestByTechnicalName(String technicalName) {
    return otaUpdateRepository.findLatestByTechnicalName(technicalName)
        .map(this::toRecord);
  }

  @Override
  public List<OtaUpdateRecord> getLatestUpdatesPerTechnicalName() {
    return otaUpdateRepository.findLatestPerTechnicalName().stream()
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
        update.getTechnicalName(),
        update.getObjectKey(),
        update.getSha256(),
        update.getSizeBytes(),
        update.isForceInstall(),
        update.getCreatedAt());
  }
}