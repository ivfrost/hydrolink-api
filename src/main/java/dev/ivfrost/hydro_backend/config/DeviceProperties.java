package dev.ivfrost.hydro_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "device")
public record DeviceProperties(
    String keySecret,
    String provisioningSecret
) {}