package dev.ivfrost.hydro_backend.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class MinioConfig {
  private final MinioProperties minioProperties;

  @Bean
  public MinioClient minioClient() {
    MinioClient client = MinioClient.builder()
        .endpoint(minioProperties.url())
        .credentials(minioProperties.rootUser(), minioProperties.rootPassword())
        .build();

    try {
      boolean found = client.bucketExists(BucketExistsArgs.builder().bucket(minioProperties.bucketName()).build());
      if (!found) {
        client.makeBucket(MakeBucketArgs.builder().bucket(minioProperties.bucketName()).build());
        log.info("Created MinIO bucket: {}", minioProperties.bucketName());
      } else {
        log.info("MinIO bucket already exists: {}", minioProperties.bucketName());
      }
      log.info("MinIO client initialized successfully with bucket: {}", minioProperties.bucketName());
    } catch (Exception e) {
      throw new RuntimeException("Could not initialize MinIO bucket: " + minioProperties.bucketName(), e);
    }

    return client;
  }
}
