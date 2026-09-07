package dev.ivfrost.hydro_backend.tokens;

import com.auth0.jwt.interfaces.Claim;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface UserTokenProvider {

  boolean isTokenValidForUserId(String token, UUID userId);

  List<TokenResponse> generateRecoveryCodes(UUID userId);

  List<TokenResponse> generateAccessAndRefreshTokens(TokenPayload payload);

  Map<String, Claim> validateTokenAndRetrieveClaims(String token);
}
