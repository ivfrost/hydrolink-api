package dev.ivfrost.hydro_backend.tokens;

import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EncryptionUtil {

  private static final SecureRandom secureRandom = new SecureRandom();
  private static final String ALGORITHM = "AES";

  public static String generateRandomString(int length) {
    byte[] randomBytes = new byte[length];
    secureRandom.nextBytes(randomBytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes).substring(0, length);
  }

  /**
   * Encrypts data deterministically using AES ECB mode. Same input + same secret = same output
   * (required for DB queries).
   */
  public String encrypt(String raw, String secret) {
    try {
      byte[] keyBytes = deriveKey(secret);
      SecretKeySpec keySpec = new SecretKeySpec(keyBytes, ALGORITHM);
      Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
      cipher.init(Cipher.ENCRYPT_MODE, keySpec);
      byte[] encrypted = cipher.doFinal(raw.getBytes());
      return new String(Hex.encode(encrypted));
    } catch (Exception e) {
      throw new RuntimeException("Encryption failed", e);
    }
  }

  /**
   * Decrypts data.
   */
  public String decrypt(String encrypted, String secret) {
    try {
      byte[] keyBytes = deriveKey(secret);
      SecretKeySpec keySpec = new SecretKeySpec(keyBytes, ALGORITHM);
      Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
      cipher.init(Cipher.DECRYPT_MODE, keySpec);
      byte[] decrypted = cipher.doFinal(Hex.decode(encrypted));
      return new String(decrypted);
    } catch (Exception e) {
      throw new RuntimeException("Decryption failed", e);
    }
  }

  /**
   * Derives a 16-byte AES key from the secret.
   */
  private byte[] deriveKey(String secret) {
    byte[] secretBytes = secret.getBytes();
    byte[] key = new byte[16];
    System.arraycopy(secretBytes, 0, key, 0, Math.min(secretBytes.length, 16));
    return key;
  }
}
