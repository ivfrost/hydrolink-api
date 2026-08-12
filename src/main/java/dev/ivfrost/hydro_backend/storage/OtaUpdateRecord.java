package dev.ivfrost.hydro_backend.storage;

import java.time.Instant;

public record OtaUpdateRecord(
    long id,
    String version,
    String technicalName,
    String objectKey,
    String sha256,
    long sizeBytes,
    boolean forceInstall,
    Instant createdAt) {}
