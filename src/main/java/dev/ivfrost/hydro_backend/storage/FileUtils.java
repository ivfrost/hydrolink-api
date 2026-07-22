package dev.ivfrost.hydro_backend.storage;

import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import java.util.UUID;

public class FileUtils {

  public static String generateStoredFilename(String originalFilename) {
    // Fallback if filename is empty or null
    if (originalFilename == null || originalFilename.isBlank()) {
      return UUID.randomUUID().toString();
    }

    // Extract extension safely
    String extension = FilenameUtils.getExtension(originalFilename);
    String baseName = FilenameUtils.getBaseName(originalFilename);

    // Sanitize base name (strip non-alphanumeric chars, replace spaces with underscores)
    String cleanBaseName = baseName.replaceAll("[^a-zA-Z0-9.-]", "_");

    // Truncate base name if extremely long to avoid S3 path length constraints
    if (cleanBaseName.length() > 50) {
      cleanBaseName = cleanBaseName.substring(0, 50);
    }

    // Combine: UUID + Clean Base Name + Extension
    String uuid = UUID.randomUUID().toString();

    return StringUtils.hasText(extension)
        ? String.format("%s_%s.%s", uuid, cleanBaseName, extension.toLowerCase())
        : String.format("%s_%s", uuid, cleanBaseName);
  }
}