package dev.ivfrost.hydro_backend.users;

import java.util.List;
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
   * and return its immutable {@code sub}. Cognito is passed the email directly
   * as the username, so repeated boots find the same user by looking it up with
   * {@code subForUsername(email)} rather than creating a new one each time (an
   * idempotent find-or-create). The display attributes are set here because the
   * local row needs them on first bind. Never call this outside the dev profile.
   */
  public String ensureDevUser(String email, String password, String username, String fullName) {
    try {
      return subForUsername(email);
    } catch (UserNotFoundException notFound) {
      List<AttributeType> attributes = List.of(
          AttributeType.builder().name("email").value(email).build(),
          AttributeType.builder().name("email_verified").value("true").build(),
          AttributeType.builder().name("preferred_username").value(username).build(),
          AttributeType.builder().name("name").value(fullName).build());

      cognitoIdentityProviderClient.adminCreateUser(
          AdminCreateUserRequest.builder()
              .userPoolId(userPoolId)
              .username(email)
              .userAttributes(attributes)
              .messageAction("SUPPRESS")
              .build());

      cognitoIdentityProviderClient.adminSetUserPassword(
          AdminSetUserPasswordRequest.builder()
              .userPoolId(userPoolId)
              .username(email)
              .password(password)
              .permanent(true)
              .build());

      return subForUsername(email);
    }
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
