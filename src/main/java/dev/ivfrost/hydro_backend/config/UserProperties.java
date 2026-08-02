package dev.ivfrost.hydro_backend.config;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

@ConfigurationProperties(prefix = "user")
public record UserProperties(
    String jwtSecret,
    String mqttTokenPrivateKey,
    String recoverySecret,
    String tokenIssuer,
    @DurationUnit(ChronoUnit.MILLIS) Duration accessTokenExpiration,
    @DurationUnit(ChronoUnit.MILLIS) Duration refreshTokenExpiration,
    @DurationUnit(ChronoUnit.MILLIS) Duration mqttTokenExpiration
) {}