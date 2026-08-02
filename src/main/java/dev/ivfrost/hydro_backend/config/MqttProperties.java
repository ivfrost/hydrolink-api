package dev.ivfrost.hydro_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mqtt")
public record MqttProperties (
  String brokerUrl,
  String topicWildcard
) {}
