package dev.ivfrost.hydro_backend.tokens;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import dev.ivfrost.hydro_backend.config.UserProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class JWTUtil {

  private static final String AUTH_TOKEN_SUBJECT = "UserDetails";

  private final UserProperties userProperties;

  // Build JWT token for authentication
  public JWTCreator.Builder buildAccessToken(TokenPayload payload) throws JWTCreationException {
    List<String> roles = (payload.roles() == null) ? List.of() : payload.roles()
        .stream()
        .map(String::toUpperCase)
        .toList();

    return JWT.create()
        .withSubject(AUTH_TOKEN_SUBJECT)
        .withClaim("userId", String.valueOf(payload.userId()))
        .withClaim("username", payload.username())
        .withClaim("email", payload.email())
        .withClaim("roles", roles)
        .withIssuer(userProperties.tokenIssuer());
  }

  private Algorithm getAuthAlgorithm() {
    byte[] secretBytes = userProperties.jwtSecret().getBytes();
    return Algorithm.HMAC512(secretBytes);
  }

  // Sign auth JWT token with HMAC using SHA-512
  private String signAccessToken(JWTCreator.Builder builder, Duration expiration)
      throws JWTCreationException {
    try {
      Instant now = Instant.now();
      Instant expiresAt = now.plus(expiration);
      return builder
          .withIssuedAt(now)
          .withExpiresAt(expiresAt)
          .sign(getAuthAlgorithm());
    } catch (JWTCreationException e) {
      log.error("Error signing auth token", e);
      throw e;
    }
  }

  // Create auth JWT token
  public String generateAccessToken(TokenPayload payload) {
    JWTCreator.Builder builder;
    try {
      builder = buildAccessToken(payload);
    } catch (JWTCreationException e) {
      log.error("Error building JWT token", e);
      throw e;
    }
    return signAccessToken(builder, userProperties.accessTokenExpiration());
  }

  // Create long-lived auth refresh JWT token for obtaining new short-lived tokens
  public String generateRefreshToken(TokenPayload payload) {
    JWTCreator.Builder builder = buildAccessToken(payload);
    return signAccessToken(builder, userProperties.refreshTokenExpiration());
  }

  public Map<String, Claim> validateTokenAndRetrieveClaims(String token)
      throws JWTVerificationException, IllegalArgumentException {
    if (token == null || token.isBlank()) {
      throw new IllegalArgumentException("Token cannot be null or blank");
    }
    DecodedJWT jwt;
    try {
      jwt = JWT.require(getAuthAlgorithm())
          .withSubject(AUTH_TOKEN_SUBJECT)
          .withIssuer(userProperties.tokenIssuer())
          .build()
          .verify(token);
    } catch (JWTDecodeException e) {
      log.error("Invalid JWT format: {}", e.getMessage());
      throw new JWTVerificationException("Invalid JWT token format", e);
    } catch (JWTVerificationException e) {
      log.error("Error verifying JWT token: {}", e.getMessage());
      throw e;
    }
    return jwt.getClaims();
  }

  public Instant getAccessTokenExpiryDate() {
    return Instant.now().plus(userProperties.accessTokenExpiration());
  }

  public Instant getRefreshTokenExpiryDate() {
    return Instant.now().plus(userProperties.refreshTokenExpiration());
  }

}