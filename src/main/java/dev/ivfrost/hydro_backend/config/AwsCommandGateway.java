package dev.ivfrost.hydro_backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient;
import software.amazon.awssdk.services.iotdataplane.model.PublishRequest;

/**
 * Command seam backed by the AWS IoT Data Plane Publish API (IAM-signed HTTP).
 *
 * <p>The backend holds no broker session and no certificate, it only signs short-lived publish calls.
 */
@Slf4j
@Component
public class AwsCommandGateway implements CommandGateway {

  private static final String TOPIC_PREFIX = "hydro/";

  private final IotDataPlaneClient iotData;

  public AwsCommandGateway(
      Environment environment,
      @Value("${spring.cloud.aws.region.static}") String region,
      @Value("${spring.cloud.aws.credentials.access-key:}") String accessKey,
      @Value("${spring.cloud.aws.credentials.secret-key:}") String secretKey) {
    // Dev and test use explicit keys from .env. Everywhere else the client
    // resolves credentials from the deployment role through the default chain.
    AwsCredentialsProvider credentialsProvider;
    if (environment.acceptsProfiles(Profiles.of("dev", "test"))) {
      if (accessKey == null || accessKey.isBlank() || secretKey == null || secretKey.isBlank()) {
        throw new IllegalStateException(
            "Missing AWS credentials for IoT data plane in dev. Configure "
                + "spring.cloud.aws.credentials access-key/secret-key.");
      }
      credentialsProvider = StaticCredentialsProvider.create(
          AwsBasicCredentials.create(accessKey, secretKey));
    } else {
      credentialsProvider = DefaultCredentialsProvider.builder().build();
    }
    this.iotData = IotDataPlaneClient.builder()
        .region(Region.of(region))
        .credentialsProvider(credentialsProvider)
        .build();
  }

  @Override
  public void publishCommand(String deviceKey, String jsonPayload) {
    publish(deviceKey, "command", jsonPayload, false);
  }

  @Override
  public void publishAnnounce(String deviceKey, String jsonPayload, boolean retained) {
    publish(deviceKey, "announce", jsonPayload, retained);
  }

  private void publish(String deviceKey, String leaf, String jsonPayload, boolean retained) {
    String topic = TOPIC_PREFIX + deviceKey + "/" + leaf;
    iotData.publish(PublishRequest.builder()
        .topic(topic)
        .qos(1)
        .retain(retained)
        .payload(SdkBytes.fromUtf8String(jsonPayload))
        .build());
    log.debug("Published to {} retaining={}: {}", topic, retained, jsonPayload);
  }
}
