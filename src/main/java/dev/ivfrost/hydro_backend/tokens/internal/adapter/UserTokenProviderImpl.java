package dev.ivfrost.hydro_backend.tokens.internal.adapter;

import com.auth0.jwt.interfaces.Claim;
import dev.ivfrost.hydro_backend.tokens.MqttTokenPayload;
import dev.ivfrost.hydro_backend.tokens.TokenPayload;
import dev.ivfrost.hydro_backend.tokens.TokenResponse;
import dev.ivfrost.hydro_backend.tokens.internal.TokenService;
import dev.ivfrost.hydro_backend.tokens.UserTokenProvider;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class UserTokenProviderImpl implements UserTokenProvider {

  private final TokenService tokenService;

  public UserTokenProviderImpl(TokenService tokenService) {
    this.tokenService = tokenService;
  }

  @Override
  public boolean isTokenValidForUserId(String token, long userId) {
    return tokenService.isTokenValidForUserId(token, userId);
  }

  @Override
  public List<TokenResponse> generateRecoveryCodes(long userId) {
    return tokenService.generateRecoveryCodes(userId);
  }

  @Override
  public List<TokenResponse> generateAccessAndRefreshTokens(TokenPayload payload) {
    return tokenService.generateAccessAndRefreshTokens(payload);
  }

  @Override
  public TokenResponse generateMqttToken(MqttTokenPayload payload) {
    return tokenService.generateMqttToken(payload);
  }

  @Override
  public Map<String, Claim> validateTokenAndRetrieveClaims(String token) {
    return tokenService.validateTokenAndRetrieveClaims(token);
  }
}
