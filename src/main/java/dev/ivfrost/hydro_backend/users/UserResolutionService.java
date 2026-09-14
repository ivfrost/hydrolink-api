package dev.ivfrost.hydro_backend.users;

import dev.ivfrost.hydro_backend.users.internal.User;
import dev.ivfrost.hydro_backend.users.internal.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;

@Slf4j
@Service
public class UserResolutionService {

  /** How long a pending email change stays valid before it is treated as abandoned. */
  public static final Duration PENDING_EMAIL_TTL = Duration.ofHours(24);

  private final UserRepository userRepository;
  private final CognitoIdentityProviderClient cognitoIdentityProviderClient;
  private final String userPoolId;

  public UserResolutionService(
      UserRepository userRepository,
      CognitoIdentityProviderClient cognitoIdentityProviderClient,
      @Value("${aws.user.pool.id}")
      String userPoolId ) {
    this.userRepository = userRepository;
    this.cognitoIdentityProviderClient = cognitoIdentityProviderClient;
    this.userPoolId = userPoolId;
  }

  @Transactional
  public User resolveUser(String sub) {
    Optional<User> existingBySub = userRepository.findBySub(sub);
    if (existingBySub.isPresent()) {
      return existingBySub.get();
    }
    // Not bound; fetch email from Cognito
    AdminGetUserResponse response = cognitoIdentityProviderClient.adminGetUser(
        AdminGetUserRequest.builder().userPoolId(userPoolId).username(sub).build());

    String email = response.userAttributes().stream()
        .filter(attr -> attr.name().equals("email")).findFirst()
        .map(AttributeType::value)
        .orElseThrow(() -> new IllegalStateException("Cognito user has no email attribute"));

    boolean emailVerified = response.userAttributes().stream()
        .filter(attr -> attr.name().equals("email_verified")).findFirst()
        .map(attr -> Boolean.parseBoolean(attr.value()))
        .orElse(false);

    Optional<User> existingByEmail = userRepository.findByEmail(email);
    // User with email already exists in DB -> bind to provided sub
    if (existingByEmail.isPresent()) {
      User user = existingByEmail.get();
      log.info("Binding Cognito sub {} to existing user row {}", sub, user.getId());
      user.setSub(sub);
      user.setEmailVerified(emailVerified);
      return userRepository.save(user);
    }

    // User doesn't exist in our DB -> create new row with bound sub
    User newUser = User.builder().sub(sub).email(email).emailVerified(emailVerified).build();
    User saved = userRepository.save(newUser);
    log.info("Created user row {} for Cognito sub {}", saved.getId(), sub);
    return saved;
  }

  @Transactional
  public User syncVerificationState(String sub) {
    User user = userRepository.findBySub(sub)
        .orElseThrow(() -> new UserNotFoundException(
            "No user bound to this session yet. Complete sign-in first. (sub is set on first sign-in)"));

    AdminGetUserResponse response = cognitoIdentityProviderClient.adminGetUser(
        AdminGetUserRequest.builder().userPoolId(userPoolId).username(sub).build());

    Map<String, String> attrs = response.userAttributes().stream()
        .collect(Collectors.toMap(AttributeType::name, AttributeType::value));

    boolean cognitoEmailVerified = Boolean.parseBoolean(attrs.getOrDefault("email_verified", "false"));
    String cognitoEmail = attrs.get("email");

    boolean pendingMatchesCognito = user.getPendingEmail() != null
        && user.getPendingEmail().equals(cognitoEmail);

    if (cognitoEmailVerified) {
      if (pendingMatchesCognito) {
        // Cognito confirmed the requested address, promote it. This wins even
        // after the TTL: a late confirmation is still a real verification.
        log.info("Promoting pending email {} for user {}", cognitoEmail, user.getId());
        user.setEmail(cognitoEmail);
        user.setPendingEmail(null);
        user.setPendingEmailSetAt(null);
      }
      // First-time signup verification; unrelated to a requested email change
      user.setEmailVerified(true);
    } else if (isPendingEmailExpired(user)) {
      // Abandoned change: no confirmation arrived in time, so stop squatting the
      // address. Only reached when Cognito has not verified the pending value.
      log.info("Clearing expired pending email {} for user {}", user.getPendingEmail(),
          user.getId());
      user.setPendingEmail(null);
      user.setPendingEmailSetAt(null);
    }

    return userRepository.save(user);
  }

  private boolean isPendingEmailExpired(User user) {
    return pendingEmailExpired(user.getPendingEmailSetAt());
  }

  /**
   * A pending email is expired when the timestamp is missing (legacy rows) or
   * older than the TTL. Exposed so the request path can apply the same rule when
   * deciding whether an address is still claimed by someone else.
   */
  public static boolean pendingEmailExpired(Instant setAt) {
    return setAt == null || setAt.isBefore(Instant.now().minus(PENDING_EMAIL_TTL));
  }
}
