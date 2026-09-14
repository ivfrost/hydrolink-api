package dev.ivfrost.hydro_backend.users;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminSetUserPasswordRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException;

@Service
public class CognitoUserSyncService {

  private final CognitoIdentityProviderClient cognitoIdentityProviderClient;
  private final String userPoolId;

  public CognitoUserSyncService(
      CognitoIdentityProviderClient cognitoIdentityProviderClient,
      @Value("${aws.user.pool.id}") String userPoolId) {
    this.cognitoIdentityProviderClient = cognitoIdentityProviderClient;
    this.userPoolId = userPoolId;
  }

  public void syncUserEmail(String sub, String newEmail) {
    AttributeType emailAttribute = AttributeType.builder()
        .name("email")
        .value(newEmail)
        .build();

    AdminUpdateUserAttributesRequest request = AdminUpdateUserAttributesRequest.builder()
        .userPoolId(userPoolId)
        .username(sub)
        .userAttributes(emailAttribute)
        .build();

    cognitoIdentityProviderClient.adminUpdateUserAttributes(request);
  }

  /**
   * Dev seeding helper: find or create the dev Cognito user for the given email
   * and return its immutable {@code sub}. The Cognito username is derived
   * deterministically from the email so repeated boots find the same user
   * instead of creating a new one (an idempotent find-or-create). Never call
   * this outside the dev profile.
   */
  public String ensureDevUser(String email, String password) {
    String username = devUsername(email);
    try {
      return subForUsername(username);
    } catch (UserNotFoundException notFound) {
      List<AttributeType> attributes = List.of(
          AttributeType.builder().name("email").value(email).build(),
          AttributeType.builder().name("email_verified").value("true").build());

      cognitoIdentityProviderClient.adminCreateUser(
          AdminCreateUserRequest.builder()
              .userPoolId(userPoolId)
              .username(username)
              .userAttributes(attributes)
              .messageAction("SUPPRESS")
              .build());

      cognitoIdentityProviderClient.adminSetUserPassword(
          AdminSetUserPasswordRequest.builder()
              .userPoolId(userPoolId)
              .username(username)
              .password(password)
              .permanent(true)
              .build());

      return subForUsername(username);
    }
  }

  /**
   * Deterministic Cognito username for a dev user, matching the real pool shape
   * where the username is a UUID and the sub equals it. Derived from the email
   * so the same email always maps to the same username.
   */
  private String devUsername(String email) {
    return UUID.nameUUIDFromBytes(email.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8))
        .toString();
  }

  private String subForUsername(String username) {
    return cognitoIdentityProviderClient.adminGetUser(
            AdminGetUserRequest.builder().userPoolId(userPoolId).username(username).build())
        .userAttributes().stream()
        .filter(attr -> "sub".equals(attr.name()))
        .map(AttributeType::value)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Cognito user has no sub attribute"));
  }
}
