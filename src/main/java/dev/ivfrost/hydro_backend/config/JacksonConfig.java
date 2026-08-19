package dev.ivfrost.hydro_backend.config;

import dev.ivfrost.hydro_backend.ApiResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@RequiredArgsConstructor
@Configuration
public class JacksonConfig {

  private final ObjectMapper objectMapper;

  @PostConstruct
  public void init() {
    ApiResponse.setObjectMapper(objectMapper);
  }
}