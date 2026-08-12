package dev.ivfrost.hydro_backend.storage;

public record UploadResponse(
    String fileUrl,
    String objectKey,
    String sha256,
    long sizeBytes) {}