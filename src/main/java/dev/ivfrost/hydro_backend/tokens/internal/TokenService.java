package dev.ivfrost.hydro_backend.tokens.internal;

import com.auth0.jwt.interfaces.Claim;
import dev.ivfrost.hydro_backend.tokens.JWTUtil;
import dev.ivfrost.hydro_backend.tokens.RecoveryCodeUtil;
import dev.ivfrost.hydro_backend.tokens.TokenResponse;
import dev.ivfrost.hydro_backend.tokens.internal.Token.TokenType;
import dev.ivfrost.hydro_backend.users.UserTokenPayload;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class TokenService {

  private final TokenRepository tokenRepository;
  private final JWTUtil jWTUtil;

  public boolean isTokenValidForUserId(String token, long userId) {
    Token foundToken = tokenRepository.findTokenByValueAndUserId(token, userId);
    if (foundToken == null) {
      return false;
    }
    tokenRepository.delete(foundToken);
    return true;
  }

  public List<TokenResponse> generateAccessTokens(UserTokenPayload payload) {
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

  public List<TokenResponse> generateRecoveryTokens(Long userId) {
    String[] recoveryCodes = RecoveryCodeUtil.generateRecoveryCodes();

    // Save recovery codes to the database
    List<Token> tokens = Arrays.stream(recoveryCodes)
        .map(code -> {
          Token token = new Token();
          token.setValue(code);
          token.setType(TokenType.RECOVERY_CODE);
          token.setUserId(userId);
          token.setExpiryDate(null);
          return token;
        })
        .toList();
    tokenRepository.saveAll(tokens);

    return Arrays.stream(recoveryCodes)
        .map(code -> new TokenResponse(code, TokenType.RECOVERY_CODE.toString(), null, userId))
        .toList();
  }

  public Map<String, String> validateTokenAndRetrieveClaims(String token) {
    Map<String, Claim> claims = jWTUtil.validateTokenAndRetrieveClaims(token);
    return claims.entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> {
              if (entry.getValue() == null || entry.getValue().isMissing() || entry.getValue()
                  .isNull()) {
                return "";
              }
              String strValue = entry.getValue().asString();
              return strValue != null ? strValue : String.valueOf(entry.getValue());
            }
        ));
  }
}
