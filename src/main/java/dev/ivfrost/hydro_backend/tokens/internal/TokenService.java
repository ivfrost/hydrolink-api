package dev.ivfrost.hydro_backend.tokens.internal;

import com.auth0.jwt.interfaces.Claim;
import dev.ivfrost.hydro_backend.tokens.JWTUtil;
import dev.ivfrost.hydro_backend.tokens.RecoveryCodeUtil;
import dev.ivfrost.hydro_backend.tokens.TokenPayload;
import dev.ivfrost.hydro_backend.tokens.TokenResponse;
import dev.ivfrost.hydro_backend.tokens.internal.Token.TokenType;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class TokenService {

  private final TokenRepository tokenRepository;
  private final JWTUtil jWTUtil;

  public boolean isTokenValidForUserId(String token, UUID userId) {
    Token foundToken = tokenRepository.findTokenByValueAndUserId(token, userId);
    if (foundToken == null) {
      return false;
    }
    tokenRepository.delete(foundToken);
    return true;
  }

  public List<TokenResponse> generateAccessAndRefreshTokens(TokenPayload payload) {
    return List.of(
        new TokenResponse(
            jWTUtil.generateAccessToken(payload),
            TokenType.AUTH_ACCESS_TOKEN.toString(),
            jWTUtil.getAccessTokenExpiryDate(),
            payload.userId()
        ),
        new TokenResponse(
            jWTUtil.generateRefreshToken(payload),
            TokenType.AUTH_REFRESH_TOKEN.toString(),
            jWTUtil.getRefreshTokenExpiryDate(),
            payload.userId()
        )
    );
  }

  public List<TokenResponse> generateRecoveryCodes(UUID userId) {
    String[] recoveryCodes = RecoveryCodeUtil.generateRecoveryCodes();

    // Save recovery codes to the database
    List<Token> tokens = Arrays.stream(recoveryCodes)
        .map(code -> {
          Token token = new Token();
          token.setValue(code);
          token.setType(TokenType.AUTH_RECOVERY_CODE);
          token.setUserId(userId);
          token.setExpiryDate(null);
          return token;
        })
        .toList();
    tokenRepository.saveAll(tokens);

    return Arrays.stream(recoveryCodes)
        .map(code -> new TokenResponse(code, TokenType.AUTH_RECOVERY_CODE.toString(), null, userId))
        .toList();
  }

  public Map<String, Claim> validateTokenAndRetrieveClaims(String token) {
    return jWTUtil.validateTokenAndRetrieveClaims(token);
  }

}
