package dev.ivfrost.hydro_backend.storage.internal;

import dev.ivfrost.hydro_backend.config.MinioProperties;
import dev.ivfrost.hydro_backend.storage.DownloadedFile;
import dev.ivfrost.hydro_backend.storage.FileDownloadException;
import dev.ivfrost.hydro_backend.storage.FileUploadException;
import dev.ivfrost.hydro_backend.storage.FileUtils;
import dev.ivfrost.hydro_backend.storage.UploadResponse;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import java.io.IOException;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class StorageService {

  private final MinioClient minioClient;
  private final MinioProperties minioProperties;

  public UploadResponse uploadFile(MultipartFile file, String folderPrefix) {
    // Generate clean filename using FileUtils
    String storedFilename = FileUtils.generateStoredFilename(file.getOriginalFilename());

    // Attach the virtual folder hierarchy prefix
    String objectKey = buildObjectKey(folderPrefix, storedFilename);

    try (InputStream inputStream = file.getInputStream()) {

      ObjectWriteResponse response = minioClient.putObject(
          PutObjectArgs.builder()
              .bucket(minioProperties.bucketName())
              .object(objectKey)
              .stream(inputStream, file.getSize(), -1L)
              .contentType(file.getContentType())
              .build()
      );

      return new UploadResponse(response.object());

    } catch (IOException e) {
      throw new FileUploadException("Failed to read incoming file stream", e);
    } catch (Exception e) {
      throw new FileUploadException("Error uploading file to MinIO storage", e);
    }
  }

  public DownloadedFile downloadFile(String objectKey) {
    try {
      GetObjectResponse response = minioClient.getObject(
          GetObjectArgs.builder()
              .bucket(minioProperties.bucketName())
              .object(objectKey)
              .build());

      // MinIO stores the content-type from putObject!
      String contentType = response.headers().get("Content-Type");
      if (contentType == null || contentType.isEmpty()) {
        contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
      }

      return new DownloadedFile(response, contentType);
    } catch (Exception e) {
      throw new FileDownloadException("Error downloading file from MinIO: " + objectKey, e);
    }
  }

  /**
   * Joins the virtual folder path and sanitized filename cleanly.
   * Example: "areas/HYDRO-A3VD21" + "uuid_file.pdf" -> "areas/HYDRO-A3VD21/uuid_file.pdf"
   */
  private String buildObjectKey(String folderPrefix, String filename) {
    if (!StringUtils.hasText(folderPrefix)) {
      return filename;
    }

    // Strip leading/trailing slashes to ensure consistent path formatting
    String cleanPrefix = folderPrefix.replaceAll("^/+|/+$", "");
    return cleanPrefix + "/" + filename;
  }
}