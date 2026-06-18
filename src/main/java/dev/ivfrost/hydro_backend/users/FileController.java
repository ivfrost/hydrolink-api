package dev.ivfrost.hydro_backend.users;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@ConditionalOnProperty(name = "features.blob-storage.enabled", havingValue = "true", matchIfMissing = true)
@RestController
@RequestMapping("/v1/files")
public class FileController {

  private final BlobStorageService storage;

  public FileController(BlobStorageService storage) {
    this.storage = storage;
  }

  @GetMapping("/{filename}")
  public ResponseEntity<Resource> getFile(@PathVariable String filename) {
    Resource file = storage.load(filename);

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
        .body(file);
  }
}
