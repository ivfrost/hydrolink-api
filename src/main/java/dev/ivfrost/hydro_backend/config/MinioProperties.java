package dev.ivfrost.hydro_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "minio")
public record MinioProperties(
    String url,
    String extUrl,
    String bucketName,
    String rootUser,
    String rootPassword) {}