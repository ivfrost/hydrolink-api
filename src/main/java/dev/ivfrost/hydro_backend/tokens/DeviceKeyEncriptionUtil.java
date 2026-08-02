package dev.ivfrost.hydro_backend.tokens;

import dev.ivfrost.hydro_backend.config.DeviceProperties;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DeviceKeyEncriptionUtil {

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final String ALGORITHM = "AES";
  private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
  private static final int KEY_LENGTH_BYTES = 16;

  private final SecretKeySpec keySpec;

  public DeviceKeyEncriptionUtil(DeviceProperties deviceProperties) {
    this.keySpec = deriveKeySpec(deviceProperties.keySecret());
  }

  public static String generateRandomString(int length) {
    byte[] randomBytes = new byte[length];
    SECURE_RANDOM.nextBytes(randomBytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes).substring(0, length);
  }

  /**
   * Encrypts data deterministically using AES ECB mode. Same input + same secret = same output
   * (required for DB queries).
   */
  public String encrypt(String raw) {
    return encrypt(raw, keySpec);
  }

  /**
   * Encrypts data with a custom secret (used for recovery codes - different secret than devices).
   */
  public String encrypt(String raw, String customSecret) {
    return encrypt(raw, deriveKeySpec(customSecret));
  }

  /**
   * Decrypts data.
   */
  public String decrypt(String encrypted) {
    return decrypt(encrypted, keySpec);
  }

  /**
   * Decrypts data with a custom secret (used for recovery codes - different secret than devices).
   */
  public String decrypt(String encrypted, String customSecret) {
    return decrypt(encrypted, deriveKeySpec(customSecret));
  }

  private String encrypt(String raw, SecretKeySpec key) {
    try {
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      cipher.init(Cipher.ENCRYPT_MODE, key);
      byte[] encrypted = cipher.doFinal(raw.getBytes(StandardCharsets.UTF_8));
      return new String(Hex.encode(encrypted));
    } catch (Exception e) {
      throw new RuntimeException("Encryption failed", e);
    }
  }

  private String decrypt(String encrypted, SecretKeySpec key) {
    try {
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      cipher.init(Cipher.DECRYPT_MODE, key);
      byte[] decrypted = cipher.doFinal(Hex.decode(encrypted));
      return new String(decrypted, StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new RuntimeException("Decryption failed", e);
    }
  }

  /**
   * Derives a 16-byte AES key from the secret.
   */
  private SecretKeySpec deriveKeySpec(String secret) {
    byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
    byte[] key = new byte[KEY_LENGTH_BYTES];
    System.arraycopy(secretBytes, 0, key, 0, Math.min(secretBytes.length, KEY_LENGTH_BYTES));
    return new SecretKeySpec(key, ALGORITHM);
  }
}