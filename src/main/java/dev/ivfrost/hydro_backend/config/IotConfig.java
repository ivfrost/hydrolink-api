package dev.ivfrost.hydro_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iot.IotClient;

@Configuration
public class IotConfig {

  @Bean
  IotClient iotClient(
      Environment environment,
      @Value("${spring.cloud.aws.region.static}") String region,
      @Value("${spring.cloud.aws.credentials.access-key:}") String accessKey,
      @Value("${spring.cloud.aws.credentials.secret-key:}") String secretKey
  ) {
    AwsCredentialsProvider credentialsProvider;
    if (environment.acceptsProfiles(Profiles.of("dev", "test"))) {
      if (accessKey == null || accessKey.isBlank() || secretKey == null || secretKey.isBlank()) {
        throw new IllegalStateException(
            "Missing AWS credentials for IoT control plane in dev. Configure "
                + "spring.cloud.aws.credentials.access-key/secret-key.");
      }
      credentialsProvider = StaticCredentialsProvider.create(
          AwsBasicCredentials.create(accessKey, secretKey));
    } else {
      credentialsProvider = DefaultCredentialsProvider.create();
    }

    return IotClient.builder()
        .region(Region.of(region))
        .credentialsProvider(credentialsProvider)
        .build();
  }
}