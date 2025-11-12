package dev.ivfrost.hydro_backend.config;

import io.github.bucket4j.Bucket;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Configuration
public class RateLimitConfig {

    @Bean
    public ConcurrentMap<String, Bucket> buckets() {
        return new ConcurrentHashMap<>();
    }
}
