package dev.ivfrost.hydro_backend.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MinioConfig {
  @Value("${minio.url}")
  private String url;

  @Value("${minio.root.user}")
  private String rootUser;

  @Value("${minio.root.password}")
  private String rootPassword;

  @Value("${minio.bucket.name}")
  private String bucketName;

  @Bean
  public MinioClient minioClient() {
    MinioClient client = MinioClient.builder()
        .endpoint(url)
        .credentials(rootUser, rootPassword)
        .build();

    try {
      boolean found = client.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
      if (!found) {
        client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        log.info("Created MinIO bucket: {}", bucketName);
      } else {
        log.info("MinIO bucket already exists: {}", bucketName);
      }
      log.info("MinIO client initialized successfully with bucket: {}", bucketName);
    } catch (Exception e) {
      throw new RuntimeException("Could not initialize MinIO bucket: " + bucketName, e);
    }

    return client;
  }
}
