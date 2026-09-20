package dev.ivfrost.hydro_backend.users;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cognitoidentity.CognitoIdentityClient;
import software.amazon.awssdk.services.cognitoidentity.model.GetIdRequest;

/**
 * Resolves the caller's Cognito Identity Pool identity id from their User Pool ID token.
 * Server-side only: the identity id must not be taken from the client, or a caller could
 * attach or detach IoT policies on someone else's identity.
 */
@Slf4j
@Service
public class CognitoIdentityResolver {

  private final CognitoIdentityClient cognitoIdentityClient;
  private final String identityPoolId;
  private final String loginProvider;

  public CognitoIdentityResolver(
      CognitoIdentityClient cognitoIdentityClient,
      @Value("${aws.identity.pool.id}") String identityPoolId,
      @Value("${spring.cloud.aws.region.static}") String region,
      @Value("${aws.user.pool.id}") String userPoolId) {
    this.cognitoIdentityClient = cognitoIdentityClient;
    this.identityPoolId = identityPoolId;
    this.loginProvider = "cognito-idp." + region + ".amazonaws.com/" + userPoolId;
  }

  /**
   * Returns the Identity Pool identity id for the given User Pool ID token, in the
   * region-qualified form ({@code <region>:<uuid>}) that IoT policy targets require.
   */
  public String resolveIdentityId(String idToken) {
    if (idToken == null || idToken.isBlank()) {
      throw new IllegalStateException("Cannot resolve a Cognito identity without an ID token");
    }

    String identityId = cognitoIdentityClient.getId(
        GetIdRequest.builder()
            .identityPoolId(identityPoolId)
            .logins(Map.of(loginProvider, idToken))
            .build()).identityId();

    if (identityId == null || identityId.isBlank()) {
      throw new IllegalStateException("Cognito returned no identity id for the caller");
    }

    log.debug("Resolved Cognito identity {} for login provider {}", identityId, loginProvider);
    return identityId;
  }
}
