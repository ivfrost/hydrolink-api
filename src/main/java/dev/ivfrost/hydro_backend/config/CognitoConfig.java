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
import software.amazon.awssdk.services.cognitoidentity.CognitoIdentityClient;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

@Configuration
public class CognitoConfig {

  @Bean
  CognitoIdentityProviderClient cognitoIdentityProviderClient(
      Environment environment,
      @Value("${spring.cloud.aws.region.static}") String region,
      @Value("${spring.cloud.aws.credentials.access-key:}") String accessKey,
      @Value("${spring.cloud.aws.credentials.secret-key:}") String secretKey
  ) {
    return CognitoIdentityProviderClient.builder()
        .region(Region.of(region))
        .credentialsProvider(resolveCredentials(environment, accessKey, secretKey))
        .build();
  }

  @Bean
  CognitoIdentityClient cognitoIdentityClient(
      Environment environment,
      @Value("${spring.cloud.aws.region.static}") String region,
      @Value("${spring.cloud.aws.credentials.access-key:}") String accessKey,
      @Value("${spring.cloud.aws.credentials.secret-key:}") String secretKey
  ) {
    return CognitoIdentityClient.builder()
        .region(Region.of(region))
        .credentialsProvider(resolveCredentials(environment, accessKey, secretKey))
        .build();
  }

  /**
   * Dev and test use explicit keys from .env. Everywhere else the client resolves
   * credentials from the deployment role through the default chain.
   */
  private AwsCredentialsProvider resolveCredentials(
      Environment environment, String accessKey, String secretKey) {
    if (environment.acceptsProfiles(Profiles.of("dev", "test"))) {
      if (accessKey == null || accessKey.isBlank() || secretKey == null || secretKey.isBlank()) {
        throw new IllegalStateException(
            "Missing AWS credentials for Cognito in dev. Configure "
                + "spring.cloud.aws.credentials.access-key/secret-key.");
      }
      return StaticCredentialsProvider.create(
          AwsBasicCredentials.create(accessKey, secretKey));
    }
    return DefaultCredentialsProvider.create();
  }
}
