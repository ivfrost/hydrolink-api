package dev.ivfrost.hydro_backend.storage.internal;

import dev.ivfrost.hydro_backend.ApiResponse;
import dev.ivfrost.hydro_backend.storage.DownloadedFile;
import dev.ivfrost.hydro_backend.storage.UploadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.InputStream;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Tag(name = "Storage Module", description = "Endpoints for file storage operations")
@AllArgsConstructor
@RestController
@RequestMapping("/v1")
public class StorageController {

  private final StorageService storageService;

  @Operation(
      summary = "Upload a file for a specific area (authenticated users only)",
      description = "Uploads a file directly to MinIO storage for a specific area using multipart form data."
  )
  @PreAuthorize("isAuthenticated()")
  @PostMapping(path = "/storage/areas/{areaId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResponse<UploadResponse>> uploadFileForArea(
      @Parameter(description = "Binary file stream sent from client app")
      @RequestPart("file") MultipartFile file,
      @Parameter(description = "ID of the area to associate the uploaded file with")
      @PathVariable String areaId
  ) {
    log.debug("Received file upload request for area {}: {}", areaId, file.getOriginalFilename());

    if (file.isEmpty()) {
      throw new IllegalArgumentException("Cannot upload an empty file");
    }

    String folderPrefix = String.format("areas/%s", areaId);
    UploadResponse uploadResponse = storageService.uploadFile(file, folderPrefix);

    return ResponseEntity.ok(
        ApiResponse.success(
            HttpStatus.OK,
            "File uploaded successfully for area " + areaId,
            uploadResponse
        )
    );
  }

  @Operation(
      summary = "Upload a file for a specific station in an area",
      description = "Uploads a file directly to MinIO storage for a specific station within an area using multipart form data."
  )
  @PostMapping(path = "/storage/areas/{areaId}/stations/{stationId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResponse<UploadResponse>> uploadFileForStation(
      @Parameter(description = "Binary file stream sent from client app")
      @RequestPart("file") MultipartFile file,
      @Parameter(description = "ID of the area to associate the uploaded file with")
      @PathVariable String areaId,
      @Parameter(description = "ID of the station to associate the uploaded file with")
      @PathVariable String stationId
  ) {
    log.debug("Received file upload request for area {} and station {}: {}", areaId, stationId, file.getOriginalFilename());

    if (file.isEmpty()) {
      throw new IllegalArgumentException("Cannot upload an empty file");
    }

    String folderPrefix = String.format("areas/%s/stations/%s", areaId, stationId);
    UploadResponse uploadResponse = storageService.uploadFile(file, folderPrefix);

    return ResponseEntity.ok(
        ApiResponse.success(
            HttpStatus.OK,
            "File uploaded successfully for area " + areaId + " and station " + stationId,
            uploadResponse
        )
    );
  }

  @Operation(
      summary = "Stream / Download a file",
      description = "Fetches a file from private storage using its key and streams it directly to the client."
  )
  @GetMapping("/storage/files")
  public ResponseEntity<Resource> getFile(
      @Parameter(description = "Storage key of the object to retrieve", example = "areas/3821/uuid_map.png")
      @RequestParam("key") String objectKey
  ) {
    log.debug("Received request to retrieve file key: {}", objectKey);

    DownloadedFile file = storageService.downloadFile(objectKey);
    InputStreamResource resource = new InputStreamResource(file.inputStream());

    String displayFilename = objectKey.contains("_")
        ? objectKey.substring(objectKey.lastIndexOf('_') + 1)
        : "file";

    String contentDisposition = String.format("inline; filename=\"%s\"", displayFilename);

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
        .contentType(MediaType.parseMediaType(file.contentType())) // <-- Dynamic MIME type
        .body(resource);
  }
}