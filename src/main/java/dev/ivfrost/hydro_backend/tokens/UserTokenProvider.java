package dev.ivfrost.hydro_backend.tokens;

import com.auth0.jwt.interfaces.Claim;
import java.util.List;
import java.util.Map;

public interface UserTokenProvider {

  boolean isTokenValidForUserId(String token, long userId);

  List<TokenResponse> generateRecoveryTokens(long userId);

  List<TokenResponse> generateAccessAndRefreshTokens(TokenPayload payload);

  TokenResponse generateMqttToken(MqttTokenPayload payload);

  Map<String, Claim> validateTokenAndRetrieveClaims(String token);
}
