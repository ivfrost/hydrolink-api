package dev.ivfrost.hydro_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "seed")
public record SeedProperties(
    String adminEmail,
    String adminPassword,
    String device1Key,
    String device2Key,
    String device1Secret,
    String device2Secret
) {}
