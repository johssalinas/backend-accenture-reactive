package co.com.accenture.redis.cache;

import co.com.accenture.redis.config.RedisConnectionProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

public abstract class GenericRedisCacheAdapter<T> {

    private static final String DEFAULT_KEY_PREFIX = "accenture";
    private static final Duration DEFAULT_BY_ID_TTL = Duration.ofMinutes(10);
    private static final Duration DEFAULT_ALL_TTL = Duration.ofMinutes(2);

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisConnectionProperties properties;
    private final String resourceName;
    private final Class<T> elementType;
    private final TypeReference<List<T>> listType;

    protected GenericRedisCacheAdapter(ReactiveStringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            RedisConnectionProperties properties,
            String resourceName,
            Class<T> elementType,
            TypeReference<List<T>> listType) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.resourceName = resourceName;
        this.elementType = elementType;
        this.listType = listType;
    }

    protected Mono<T> getByIdResource(UUID id) {
        if (id == null) {
            return Mono.empty();
        }

        return redisTemplate.opsForValue()
                .get(byIdKey(id))
                .flatMap(this::deserializeItem)
                .onErrorResume(error -> Mono.empty());
    }

    protected Mono<Void> putByIdResource(UUID id, T resource) {
        if (id == null || resource == null) {
            return Mono.empty();
        }

        return serialize(resource)
                .flatMap(payload -> redisTemplate.opsForValue()
                        .set(byIdKey(id), payload, byIdTtl())
                        .then())
                .onErrorResume(error -> Mono.empty());
    }

    protected Flux<T> getAllResources() {
        return redisTemplate.opsForValue()
                .get(allKey())
                .flatMapMany(payload -> deserializeList(payload).flatMapMany(Flux::fromIterable))
                .onErrorResume(error -> Flux.empty());
    }

    protected Mono<Void> putAllResources(List<T> resources) {
        if (resources == null || resources.isEmpty()) {
            return evictAllResources();
        }

        return serialize(resources)
                .flatMap(payload -> redisTemplate.opsForValue()
                        .set(allKey(), payload, allTtl())
                        .then())
                .onErrorResume(error -> Mono.empty());
    }

    protected Mono<Void> evictByIdResource(UUID id) {
        if (id == null) {
            return Mono.empty();
        }

        return redisTemplate.delete(byIdKey(id))
                .then()
                .onErrorResume(error -> Mono.empty());
    }

    protected Mono<Void> evictAllResources() {
        return redisTemplate.delete(allKey())
                .then()
                .onErrorResume(error -> Mono.empty());
    }

    private Mono<String> serialize(Object value) {
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(value));
    }

    private Mono<T> deserializeItem(String payload) {
        return Mono.fromCallable(() -> objectMapper.readValue(payload, elementType));
    }

    private Mono<List<T>> deserializeList(String payload) {
        return Mono.fromCallable(() -> objectMapper.readValue(payload, listType));
    }

    private String byIdKey(UUID id) {
        return keyPrefix() + ":" + resourceName + ":id:" + id;
    }

    private String allKey() {
        return keyPrefix() + ":" + resourceName + ":all:v1";
    }

    private String keyPrefix() {
        String configuredPrefix = properties.keyPrefix();
        return configuredPrefix == null || configuredPrefix.isBlank() ? DEFAULT_KEY_PREFIX : configuredPrefix;
    }

    private Duration byIdTtl() {
        return properties.cacheByIdTtl() == null ? DEFAULT_BY_ID_TTL : properties.cacheByIdTtl();
    }

    private Duration allTtl() {
        return properties.cacheAllTtl() == null ? DEFAULT_ALL_TTL : properties.cacheAllTtl();
    }
}