package dev.ivfrost.hydro_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "api")
public record ApiProperties(
  String mqttClientId,
  String mqttUsername,
  String mqttPassword
) {}
