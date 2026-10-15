package dev.ivfrost.hydro_backend.config;

import dev.ivfrost.hydro_backend.common.ApiResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@RequiredArgsConstructor
@Configuration
public class JacksonConfig {

  private final ObjectMapper objectMapper;

  @PostConstruct
  public void init() {
    ApiResponse.setObjectMapper(objectMapper);
  }

  @Bean
  ObjectMapper objectMapper() {
    return JsonMapper.builder().findAndAddModules().build();
  }
}