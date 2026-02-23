package co.com.accenture.redis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;

@Configuration
public class RedisConfig {

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 6379;
    private static final int DEFAULT_DATABASE = 0;
    private static final Duration DEFAULT_COMMAND_TIMEOUT = Duration.ofSeconds(3);

    @Bean
    public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory(RedisConnectionProperties properties) {
        RedisStandaloneConfiguration standaloneConfiguration = new RedisStandaloneConfiguration(
                defaultHost(properties.host()),
                defaultPort(properties.port()));
        standaloneConfiguration.setDatabase(defaultDatabase(properties.database()));
        if (properties.password() != null && !properties.password().isBlank()) {
            standaloneConfiguration.setPassword(RedisPassword.of(properties.password()));
        }

        LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfiguration = LettuceClientConfiguration
                .builder()
                .commandTimeout(defaultCommandTimeout(properties.commandTimeout()));

        if (Boolean.TRUE.equals(properties.ssl())) {
            clientConfiguration.useSsl();
        }

        return new LettuceConnectionFactory(standaloneConfiguration, clientConfiguration.build());
    }

    @Bean
    public ReactiveStringRedisTemplate reactiveStringRedisTemplate(
            @Qualifier("reactiveRedisConnectionFactory") ReactiveRedisConnectionFactory connectionFactory) {
        return new ReactiveStringRedisTemplate(connectionFactory);
    }

    @Bean
    public ObjectMapper redisObjectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    private static String defaultHost(String host) {
        return (host == null || host.isBlank()) ? DEFAULT_HOST : host;
    }

    private static int defaultPort(Integer port) {
        return port == null ? DEFAULT_PORT : port;
    }

    private static int defaultDatabase(Integer database) {
        return database == null ? DEFAULT_DATABASE : database;
    }

    private static Duration defaultCommandTimeout(Duration commandTimeout) {
        return commandTimeout == null ? DEFAULT_COMMAND_TIMEOUT : commandTimeout;
    }
}
