package dev.ivfrost.hydro_backend.devices;

import dev.ivfrost.hydro_backend.config.DeviceProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Symmetric encryption for device secrets.
 *
 * <p>Ciphertext uses AES-GCM (authenticated, random IV per call), so the same plaintext produces a
 * different ciphertext every time. Equality lookups therefore do not run against the ciphertext;
 * they run against a deterministic {@link #fingerprint(String)} (HMAC-SHA256) stored in its own
 * column. This is the standard "blind index" pattern: secure encryption plus a searchable tag.
 */
@Slf4j
@Component
public class DeviceKeyEncryptionUtil {

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final String CIPHER = "AES/GCM/NoPadding";
  private static final String MAC = "HmacSHA256";
  private static final int GCM_IV_LENGTH_BYTES = 12;
  private static final int GCM_TAG_LENGTH_BITS = 128;

  private final SecretKeySpec cipherKey;
  private final SecretKeySpec macKey;

  public DeviceKeyEncryptionUtil(DeviceProperties deviceProperties) {
    this.cipherKey = deriveCipherKey(deviceProperties.keySecret());
    this.macKey = deriveMacKey(deviceProperties.keySecret());
  }

  public static String generateRandomString(int length) {
    byte[] randomBytes = new byte[length];
    SECURE_RANDOM.nextBytes(randomBytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes).substring(0, length);
  }

  /**
   * Encrypts data with AES-GCM under the device secret key. A fresh random IV is used on every call,
   * so the output is not deterministic.
   */
  public String encrypt(String raw) {
    return encrypt(raw, cipherKey);
  }

  /**
   * Encrypts data with a custom secret (used for recovery codes, a different secret than devices).
   */
  public String encrypt(String raw, String customSecret) {
    return encrypt(raw, deriveCipherKey(customSecret));
  }

  /**
   * Decrypts data produced by {@link #encrypt(String)}.
   */
  public String decrypt(String encrypted) {
    return decrypt(encrypted, cipherKey);
  }

  /**
   * Decrypts data produced by {@link #encrypt(String, String)}.
   */
  public String decrypt(String encrypted, String customSecret) {
    return decrypt(encrypted, deriveCipherKey(customSecret));
  }

  /**
   * Deterministic, non-reversible tag used for equality lookups over encrypted values. Same input
   * and same secret always produce the same tag, but the tag cannot be turned back into the input and
   * reveals nothing about it, unlike deterministic encryption.
   */
  public String fingerprint(String raw) {
    return fingerprint(raw, macKey);
  }

  /**
   * Fingerprint with a custom secret, matching {@link #encrypt(String, String)}.
   */
  public String fingerprint(String raw, String customSecret) {
    return fingerprint(raw, deriveMacKey(customSecret));
  }

  private String encrypt(String raw, SecretKeySpec key) {
    try {
      byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
      SECURE_RANDOM.nextBytes(iv);

      Cipher cipher = Cipher.getInstance(CIPHER);
      cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
      byte[] cipherText = cipher.doFinal(raw.getBytes(StandardCharsets.UTF_8));

      // Prefix the IV so decryption does not need a separate column for it.
      byte[] combined = new byte[iv.length + cipherText.length];
      System.arraycopy(iv, 0, combined, 0, iv.length);
      System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
      return Base64.getUrlEncoder().withoutPadding().encodeToString(combined);
    } catch (Exception e) {
      throw new IllegalStateException("Encryption failed", e);
    }
  }

  private String decrypt(String encrypted, SecretKeySpec key) {
    try {
      byte[] combined = Base64.getUrlDecoder().decode(encrypted);
      byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
      byte[] cipherText = new byte[combined.length - GCM_IV_LENGTH_BYTES];
      System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH_BYTES);
      System.arraycopy(combined, GCM_IV_LENGTH_BYTES, cipherText, 0, cipherText.length);

      Cipher cipher = Cipher.getInstance(CIPHER);
      cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
      byte[] decrypted = cipher.doFinal(cipherText);
      return new String(decrypted, StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new IllegalStateException("Decryption failed", e);
    }
  }

  private String fingerprint(String raw, SecretKeySpec key) {
    try {
      Mac mac = Mac.getInstance(MAC);
      mac.init(key);
      byte[] tag = mac.doFinal(raw.getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(tag);
    } catch (Exception e) {
      throw new IllegalStateException("Fingerprinting failed", e);
    }
  }

  /** Derives a 256-bit AES key from the secret via SHA-256, avoiding truncation. */
  private SecretKeySpec deriveCipherKey(String secret) {
    return new SecretKeySpec(sha256(secret), "AES");
  }

  /** Derives a 256-bit HMAC key from the secret via a domain-separated SHA-256. */
  private SecretKeySpec deriveMacKey(String secret) {
    return new SecretKeySpec(sha256("hmac:" + secret), MAC);
  }

  private byte[] sha256(String value) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
    } catch (Exception e) {
      throw new IllegalStateException("Key derivation failed", e);
    }
  }
}
