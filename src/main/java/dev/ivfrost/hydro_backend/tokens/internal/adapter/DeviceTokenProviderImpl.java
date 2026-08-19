package dev.ivfrost.hydro_backend.tokens.internal.adapter;

import dev.ivfrost.hydro_backend.tokens.DeviceTokenProvider;
import dev.ivfrost.hydro_backend.tokens.MqttTokenPayload;
import dev.ivfrost.hydro_backend.tokens.TokenResponse;
import dev.ivfrost.hydro_backend.tokens.internal.TokenService;
import org.springframework.stereotype.Service;

@Service
public class DeviceTokenProviderImpl implements DeviceTokenProvider {

  private final TokenService tokenService;

  public DeviceTokenProviderImpl(TokenService tokenService) {
    this.tokenService = tokenService;
  }

  @Override
  public TokenResponse generateMqttToken(MqttTokenPayload payload) {
    return tokenService.generateMqttToken(payload);
  }

  @Override
  public void validateMqttToken(String token) {
    tokenService.validateMqttToken(token);
  }

  @Override
  public boolean validateMqttAcl(String token, String topic,  int action) {
    return tokenService.validateMqttAcl(token, topic, action);
  }

}
