package dev.ivfrost.hydro_backend.config;

import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration
public class RedisConfig {

  @Bean
  public CacheManager cacheManager(RedisConnectionFactory factory) {
    RedisSerializer<Object> serializer = RedisSerializer.json();

    RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
        .serializeValuesWith(
            RedisSerializationContext.SerializationPair.fromSerializer(serializer)
        );

    return RedisCacheManager.builder(factory)
        .cacheDefaults(config)
        .build();
  }
}