package dev.ivfrost.hydro_backend.util;

import dev.ivfrost.hydro_backend.repository.UserTokenRepository;
import dev.ivfrost.hydro_backend.service.EncoderService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

import java.security.SecureRandom;

@AllArgsConstructor
public class RecoveryCodeUtil {
    private static final String CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int RECOVERY_CODE_LENGTH = 16;

    public static String generateRecoveryCode() {
        StringBuilder code = new StringBuilder(RECOVERY_CODE_LENGTH);
        for (int i = 0; i < RECOVERY_CODE_LENGTH; i++) {
            int idx = RANDOM.nextInt(CHARSET.length());
            code.append(CHARSET.charAt(idx));
        }
        return code.toString();
    }

    public static String[] generateRecoveryCodes(int count) {
        String[] codes = new String[count];
        for (int i = 0; i < count; i++) {
            codes[i] = generateRecoveryCode();
        }
        return codes;
    }
}
