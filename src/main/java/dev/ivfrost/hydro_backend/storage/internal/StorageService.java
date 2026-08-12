package dev.ivfrost.hydro_backend.storage.internal;

import dev.ivfrost.hydro_backend.config.MinioProperties;
import dev.ivfrost.hydro_backend.storage.DownloadedFile;
import dev.ivfrost.hydro_backend.storage.FileDownloadException;
import dev.ivfrost.hydro_backend.storage.FileUploadException;
import dev.ivfrost.hydro_backend.storage.FileUtils;
import dev.ivfrost.hydro_backend.storage.UploadResponse;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.Http;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

  /** Presigned URLs are valid for 7 days (MinIO max). */
  private static final long PRESIGNED_URL_EXPIRY_SECONDS = Duration.ofDays(7).toSeconds();

  private final MinioClient minioClient;
  private final MinioProperties minioProperties;

  public UploadResponse uploadFile(MultipartFile file, String folderPrefix) {
    // Generate clean filename using FileUtils
    String storedFilename = FileUtils.generateStoredFilename(file.getOriginalFilename());

    // Attach the virtual folder hierarchy prefix
    String objectKey = buildObjectKey(folderPrefix, storedFilename);

    return storeFile(file, objectKey);
  }

  /**
   * Uploads a firmware binary under a deterministic, reconstructible object key:
   * {@code firmware/{technicalName}/{version}.bin}. Because the key contains no random
   * UUID, it can be recomputed later to mint fresh presigned URLs for re-dispatch.
   */
  public UploadResponse uploadFirmwareFile(MultipartFile file, String technicalName, String version) {
    String objectKey = "firmware/" + technicalName + "/" + version + ".bin";
    return storeFile(file, objectKey);
  }

  /**
   * Mints a fresh presigned GET URL for the given object key. The URL is rewritten so the
   * host is the externally reachable {@code minio.extUrl} rather than the internal endpoint
   * the MinIO client was configured with.
   */
  public String generatePresignedUrl(String objectKey) {
    try {
      String presignedUrl = minioClient.getPresignedObjectUrl(
          GetPresignedObjectUrlArgs.builder()
              .method(Http.Method.GET)
              .bucket(minioProperties.bucketName())
              .object(objectKey)
              .expiry((int) PRESIGNED_URL_EXPIRY_SECONDS)
              .build()
      );
      return rewriteEndpointToExternalUrl(presignedUrl);
    } catch (Exception e) {
      throw new FileUploadException("Error generating presigned URL for object: " + objectKey, e);
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

  private UploadResponse storeFile(MultipartFile file, String objectKey) {
    byte[] bytes;
    try {
      bytes = file.getBytes();
    } catch (IOException e) {
      throw new FileUploadException("Failed to read incoming file stream", e);
    }

    String sha256 = sha256Hex(bytes);

    try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
      minioClient.putObject(
          PutObjectArgs.builder()
              .bucket(minioProperties.bucketName())
              .object(objectKey)
              .stream(inputStream, (long) bytes.length, -1L)
              .contentType(file.getContentType())
              .build()
      );
    } catch (IOException e) {
      throw new FileUploadException("Failed to upload file to MinIO storage", e);
    } catch (Exception e) {
      throw new FileUploadException("Error uploading file to MinIO storage", e);
    }

    String presignedUrl = generatePresignedUrl(objectKey);
    return new UploadResponse(presignedUrl, objectKey, sha256, bytes.length);
  }

  /**
   * MinIO builds presigned URLs against the endpoint the client was configured with
   * ({@code minio.url}, e.g. an internal docker network host). Rewrite the scheme+host+port
   * portion using {@code minio.extUrl} so devices can actually reach the bucket.
   */
  private String rewriteEndpointToExternalUrl(String presignedUrl) {
    try {
      URI uri = new URI(presignedUrl);
      String externalBase = minioProperties.extUrl().replaceAll("/+$", "");
      return externalBase + uri.getRawPath() + (uri.getQuery() != null ? "?" + uri.getQuery() : "");
    } catch (URISyntaxException e) {
      log.warn("Could not rewrite presigned URL '{}' to external URL; returning as-is", presignedUrl, e);
      return presignedUrl;
    }
  }

  private String sha256Hex(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (NoSuchAlgorithmException e) {
      throw new FileUploadException("SHA-256 algorithm is not available", e);
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