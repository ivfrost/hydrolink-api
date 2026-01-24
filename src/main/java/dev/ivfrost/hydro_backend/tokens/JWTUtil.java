package dev.ivfrost.hydro_backend.tokens;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import dev.ivfrost.hydro_backend.users.UserMqttTokenPayload;
import dev.ivfrost.hydro_backend.users.UserTokenPayload;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class JWTUtil {

  private static final String AUTH_TOKEN_SUBJECT = "UserDetails";
  private static final String ISSUER = "HydroAPI";
  @Value("${jwt.secret}")
  private String jwtSecret;
  @Value("${jwt.access.expiration.ms}")
  private Long jwtAccessExpirationMs;
  @Value("${jwt.refresh.expiration.ms}")
  private Long jwtRefreshExpirationMs;
  @Value("${mqtt.jwt.private.key.path}")
  private String mqttJwtPrivateKeyPath;
  @Value("${mqtt.jwt.expiration-ms}")
  private Long mqttJwtExpirationMs;

  // Build JWT token for authentication
  public JWTCreator.Builder buildAccessToken(UserTokenPayload payload) throws JWTCreationException {
    List<String> roles = (payload.roles() == null) ? List.of() : payload.roles()
        .stream()
        .map(String::toUpperCase)
        .toList();

    return JWT.create()
        .withSubject(AUTH_TOKEN_SUBJECT)
        .withClaim("username", payload.username())
        .withClaim("email", payload.email())
        .withClaim("roles", roles)
        .withClaim("preferredLanguage", payload.preferredLanguage())
        .withIssuer(ISSUER);
  }

  // Sign auth JWT token with HMAC using SHA-512
  private String signAccessToken(JWTCreator.Builder builder, Long expirationMs)
      throws JWTCreationException {
    try {
      Instant now = Instant.now();
      Instant expiresAt = now.plus(expirationMs, ChronoUnit.MILLIS);
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
  public String generateAccessToken(UserTokenPayload payload) {
    JWTCreator.Builder builder;
    try {
      builder = buildAccessToken(payload);
    } catch (JWTCreationException e) {
      log.error("Error building JWT token", e);
      throw e;
    }
    return signAccessToken(builder, jwtAccessExpirationMs);
  }

  private Algorithm getAuthAlgorithm() {
    byte[] secretBytes = jwtSecret.getBytes();
    return Algorithm.HMAC512(secretBytes);
  }

  private Algorithm getMqttAlgorithm() {
    RSAPrivateKey privateKey = loadPrivateKeyFromFile(mqttJwtPrivateKeyPath);
    return Algorithm.RSA256(null, privateKey);
  }

  // Build JWT token for MQTT authentication
  public JWTCreator.Builder buildMqttToken(UserMqttTokenPayload payload)
      throws JWTCreationException {
    log.debug("Building MQTT token for userId: {}, topics: {}", payload.userId(), payload.topics());
    return JWT.create()
        .withSubject(payload.userId().toString())
        .withClaim("subs", payload.topics())
        .withClaim("publ", payload.topics())
        .withIssuer(ISSUER);
  }

  // Sign auth MQTT JWT token with RSA using SHA-256
  private String signMqttToken(JWTCreator.Builder builder, Long expirationMs)
      throws JWTCreationException {
    try {
      Instant now = Instant.now();
      Instant expiresAt = now.plus(expirationMs, ChronoUnit.MILLIS);
      return builder
          .withIssuedAt(now)
          .withExpiresAt(expiresAt)
          .sign(getMqttAlgorithm());
    } catch (JWTCreationException e) {
      log.error("Error signing MQTT token", e);
      throw e;
    }
  }

  // Create long-lived auth refresh JWT token for obtaining new short-lived tokens
  public String generateRefreshToken(UserTokenPayload payload) {
    JWTCreator.Builder builder = buildAccessToken(payload);
    return signAccessToken(builder, jwtRefreshExpirationMs);
  }

  // Create a short-lived MQTT auth JWT token
  public String generateMqttToken(UserMqttTokenPayload payload) {
    JWTCreator.Builder builder;
    try {
      builder = buildMqttToken(payload);
    } catch (JWTCreationException e) {
      log.error("Error building MQTT JWT token", e);
      throw e;
    }
    return signMqttToken(builder, mqttJwtExpirationMs);
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
          .withIssuer(ISSUER)
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
    return Instant.now().plus(jwtAccessExpirationMs, ChronoUnit.MILLIS);
  }

  public Instant getRefreshTokenExpiryDate() {
    return Instant.now().plus(jwtRefreshExpirationMs, ChronoUnit.MILLIS);
  }

  private RSAPrivateKey loadPrivateKeyFromFile(String path) {
    if (path == null || path.isBlank()) {
      throw new IllegalArgumentException("Private key file path cannot be null or blank");
    }
    File keyFile = new File(path);
    if (!keyFile.exists() || !keyFile.isFile()) {
      throw new IllegalArgumentException("Invalid private key file path: " + path);
    }
    try {
      byte[] keyBytes = Files.readAllBytes(keyFile.toPath());
      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
      return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
    } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new RuntimeException(e);
    }
  }
}