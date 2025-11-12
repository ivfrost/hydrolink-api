package dev.ivfrost.hydro_backend.service;

import dev.ivfrost.hydro_backend.entity.UserToken;
import dev.ivfrost.hydro_backend.exception.RecoveryTokenMismatchException;
import dev.ivfrost.hydro_backend.exception.RecoveryTokenNotFoundException;
import dev.ivfrost.hydro_backend.repository.UserTokenRepository;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Data
@Service
public class UserTokenService {

    private final UserTokenRepository userTokenRepository;
    private final EncoderService encoderService;

    @Value("${security-code.secret}")
    private String securityCodeSecret;

    public UserTokenService(UserTokenRepository userTokenRepository, EncoderService encoderService) {
        this.userTokenRepository = userTokenRepository;
        this.encoderService = encoderService;
    }

    public boolean isRecoveryCodeValid(String rawCode, String email) {
        String encodedCode = encoderService.hmacSha256Encoder().apply(securityCodeSecret, rawCode);
        UserToken userToken = userTokenRepository.findByTokenAndType(encodedCode, UserToken.TokenType.RECOVERY_CODE)
                .orElseThrow(() -> new RecoveryTokenNotFoundException("Recovery code not found"));
        if (!userToken.getUser().getEmail().equals(email)) {
            throw new RecoveryTokenMismatchException("Recovery code does not match the provided email");
        }
        return true;
    }
}
