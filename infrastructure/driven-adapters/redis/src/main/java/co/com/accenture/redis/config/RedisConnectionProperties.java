package co.com.accenture.redis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "adapters.redis")
public record RedisConnectionProperties(
        String host,
        Integer port,
        Integer database,
        String password,
        Boolean ssl,
        Duration connectTimeout,
        Duration commandTimeout,
        String keyPrefix,
        Duration idempotencyInProgressTtl,
        Duration idempotencyCompletedTtl,
        Duration franquiciaByIdTtl,
        Duration franquiciaAllTtl) {
}
