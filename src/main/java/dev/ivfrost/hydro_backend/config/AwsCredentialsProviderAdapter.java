package dev.ivfrost.hydro_backend.config;

import io.minio.Time;
import io.minio.credentials.Credentials;
import io.minio.credentials.Provider;
import java.time.ZonedDateTime;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;

public final class AwsCredentialsProviderAdapter implements Provider {
  private final AwsCredentialsProvider delegate;

  public AwsCredentialsProviderAdapter(AwsCredentialsProvider delegate) {
    this.delegate = delegate;
  }

  @Override
  public Credentials fetch() {
    AwsCredentials c = delegate.resolveCredentials();

    String sessionToken = null;
    Time.S3Time expiration = null;
    if (c instanceof AwsSessionCredentials session) {
      sessionToken = session.sessionToken();
      expiration = session.expirationTime()
          .map(instant -> new Time.S3Time(ZonedDateTime.ofInstant(instant, Time.UTC)))
          .orElse(null);
    }

    return new Credentials(
        c.accessKeyId(),
        c.secretAccessKey(),
        sessionToken,
        expiration);
  }
}