package dev.ivfrost.hydro_backend.tokens.internal.adapter;

import dev.ivfrost.hydro_backend.tokens.JWTUtil;
import dev.ivfrost.hydro_backend.tokens.MqttTokenProvider;
import dev.ivfrost.hydro_backend.users.UserMqttTokenPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class MqttTokenProviderImpl implements MqttTokenProvider {

  private final JWTUtil jwtUtil;

  @Override
  public String generateMqttToken(UserMqttTokenPayload payload) {
    return jwtUtil.generateMqttToken(payload);
  }
}
