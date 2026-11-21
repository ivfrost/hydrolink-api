package dev.ivfrost.hydro_backend.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class MinioConfig {
  private final MinioProperties minioProperties;

  // Prod: pointed at S3 and signed with SigV4 using the task role.

  @Bean
  public MinioClient minioClient(Environment environment) {
    MinioClient.Builder builder = MinioClient.builder()
        .endpoint(minioProperties.url())
        .region(minioProperties.bucketRegion());

    // Dev / Test: local MinIO container, using static root user and password values from .env
    if (environment.acceptsProfiles(Profiles.of("dev", "test"))) {
      if (minioProperties.rootUser() == null || minioProperties.rootUser().isBlank() ||
          minioProperties.rootPassword() == null || minioProperties.rootPassword().isBlank()) {
        throw new IllegalStateException(
            "Missing MinIO credentials for dev. Configure minio.root-user/minio.root-password.");
      }
      builder.credentials(minioProperties.rootUser(), minioProperties.rootPassword());
    // Prod: S3, authenticated as the ECS task role through the default chain
    } else {
      builder.credentialsProvider(new AwsCredentialsProviderAdapter(
          DefaultCredentialsProvider.builder().build()));
    }

    return builder.build();
  }

  @Bean
  @Profile("dev")
  public ApplicationRunner localBucketInitializer(MinioClient minioClient) {
    return args -> {
      String bucket = minioProperties.bucketName();
      try {
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
          minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
          log.info("Created local MinIO bucket {}", bucket);
        }
      } catch (Exception e) {
        log.warn("Could not ensure local MinIO bucket {}: {}", bucket, e.getMessage());
      }
    };
  }
}
