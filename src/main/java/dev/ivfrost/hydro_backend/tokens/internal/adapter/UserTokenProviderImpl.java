package dev.ivfrost.hydro_backend.tokens.internal.adapter;

import dev.ivfrost.hydro_backend.tokens.TokenResponse;
import dev.ivfrost.hydro_backend.tokens.internal.TokenService;
import dev.ivfrost.hydro_backend.users.UserTokenPayload;
import dev.ivfrost.hydro_backend.users.UserTokenProvider;
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
  public List<TokenResponse> generateRecoveryTokens(long userId) {
    return tokenService.generateRecoveryTokens(userId);
  }

  @Override
  public List<TokenResponse> generateAccessTokens(UserTokenPayload payload) {
    return tokenService.generateAccessTokens(payload);
  }

  @Override
  public Map<String, String> validateTokenAndRetrieveClaims(String token) {
    return tokenService.validateTokenAndRetrieveClaims(token);
  }
}
