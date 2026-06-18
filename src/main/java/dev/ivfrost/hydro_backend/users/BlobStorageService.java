package dev.ivfrost.hydro_backend.users;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnProperty(name = "features.blob-storage.enabled", havingValue = "true", matchIfMissing = true)
public class BlobStorageService {

  private final Path root = Paths.get("assets/uploads");

  public BlobStorageService() throws IOException {
    Files.createDirectories(root);
  }

  /**
   * Saves a file and returns the generated filename (key).
   */
  public String save(MultipartFile file, Long ownerId) throws IOException {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("File cannot be null or empty");
    }

    String ext = getExtension(file.getOriginalFilename());
    String filename = ownerId + "_" + UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);

    Path dest = root.resolve(filename);
    Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

    return filename;
  }

  /**
   * Loads a file as a Spring Resource.
   */
  public Resource load(String filename) {
    Path file = root.resolve(filename);
    return new FileSystemResource(file);
  }

  /**
   * Deletes a file if it exists.
   */
  public void delete(String filename) throws IOException {
    Files.deleteIfExists(root.resolve(filename));
  }

  /**
   * Returns a public URL for serving the file.
   */
  public String getPublicUrl(String filename) {
    return "/files/" + filename;
  }

  private String getExtension(String name) {
    if (name == null) {
      return "";
    }
    int dot = name.lastIndexOf('.');
    return dot == -1 ? "" : name.substring(dot + 1);
  }
}
