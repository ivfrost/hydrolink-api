package dev.ivfrost.hydro_backend.storage;

import java.io.InputStream;

public record DownloadedFile(InputStream inputStream, String contentType) {}