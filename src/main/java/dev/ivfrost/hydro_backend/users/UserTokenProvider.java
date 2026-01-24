package dev.ivfrost.hydro_backend.users;

import dev.ivfrost.hydro_backend.tokens.TokenResponse;
import java.util.List;
import java.util.Map;

public interface UserTokenProvider {

  boolean isTokenValidForUserId(String token, long userId);

  List<TokenResponse> generateRecoveryTokens(long userId);

  List<TokenResponse> generateAccessTokens(UserTokenPayload payload);

  Map<String, String> validateTokenAndRetrieveClaims(String token);
}
