package dev.ivfrost.hydro_backend.tokens;

import java.security.SecureRandom;

public class RecoveryCodeUtil {

  public static final int RECOVERY_CODE_LENGTH = 16;
  public static final int RECOVERY_CODE_COUNT = 5;
  private static final String CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
  private static final SecureRandom RANDOM = new SecureRandom();

  private RecoveryCodeUtil() {
  }

  public static String generateRecoveryCode() {
    StringBuilder code = new StringBuilder(RECOVERY_CODE_LENGTH);
    for (int i = 0; i < RECOVERY_CODE_LENGTH; i++) {
      int idx = RANDOM.nextInt(CHARSET.length());
      code.append(CHARSET.charAt(idx));
    }
    return code.toString();
  }

  public static String[] generateRecoveryCodes() {
    String[] codes = new String[RECOVERY_CODE_COUNT];
    for (int i = 0; i < RECOVERY_CODE_COUNT; i++) {
      codes[i] = generateRecoveryCode();
    }
    return codes;
  }

}
