package co.com.accenture.redis.cache;

import co.com.accenture.model.franquicia.Franquicia;
import co.com.accenture.model.franquicia.gateways.FranquiciaCacheRepository;
import co.com.accenture.redis.config.RedisConnectionProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class FranquiciaCacheAdapter implements FranquiciaCacheRepository {

    private static final TypeReference<List<Franquicia>> FRANQUICIA_LIST_TYPE = new TypeReference<>() {
    };
    private static final Duration DEFAULT_FRANQUICIA_BY_ID_TTL = Duration.ofMinutes(10);
    private static final Duration DEFAULT_FRANQUICIA_ALL_TTL = Duration.ofMinutes(2);
    private static final String DEFAULT_KEY_PREFIX = "accenture";

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisConnectionProperties properties;

    @Override
    public Mono<Franquicia> getById(UUID id) {
        if (id == null) {
            return Mono.empty();
        }

        return redisTemplate.opsForValue()
                .get(byIdKey(id))
                .flatMap(this::deserializeFranquicia)
                .onErrorResume(error -> Mono.empty());
    }

    @Override
    public Mono<Void> putById(Franquicia franquicia) {
        if (franquicia == null || franquicia.getId() == null) {
            return Mono.empty();
        }

        return serialize(franquicia)
                .flatMap(payload -> redisTemplate.opsForValue()
                        .set(byIdKey(franquicia.getId()), payload, franquiciaByIdTtl())
                        .then())
                .onErrorResume(error -> Mono.empty());
    }

    @Override
    public Flux<Franquicia> getAll() {
        return redisTemplate.opsForValue()
                .get(allKey())
                .flatMapMany(payload -> deserializeFranquiciaList(payload)
                        .flatMapMany(Flux::fromIterable))
                .onErrorResume(error -> Flux.empty());
    }

    @Override
    public Mono<Void> putAll(List<Franquicia> franquicias) {
        if (franquicias == null || franquicias.isEmpty()) {
            return evictAll();
        }

        return serialize(franquicias)
                .flatMap(payload -> redisTemplate.opsForValue()
                        .set(allKey(), payload, franquiciaAllTtl())
                        .then())
                .onErrorResume(error -> Mono.empty());
    }

    @Override
    public Mono<Void> evictById(UUID id) {
        if (id == null) {
            return Mono.empty();
        }

        return redisTemplate.delete(byIdKey(id))
                .then()
                .onErrorResume(error -> Mono.empty());
    }

    @Override
    public Mono<Void> evictAll() {
        return redisTemplate.delete(allKey())
                .then()
                .onErrorResume(error -> Mono.empty());
    }

    private Mono<String> serialize(Object value) {
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(value));
    }

    private Mono<Franquicia> deserializeFranquicia(String payload) {
        return Mono.fromCallable(() -> objectMapper.readValue(payload, Franquicia.class));
    }

    private Mono<List<Franquicia>> deserializeFranquiciaList(String payload) {
        return Mono.fromCallable(() -> objectMapper.readValue(payload, FRANQUICIA_LIST_TYPE));
    }

    private String byIdKey(UUID id) {
        return keyPrefix() + ":franquicia:id:" + id;
    }

    private String allKey() {
        return keyPrefix() + ":franquicia:all:v1";
    }

    private String keyPrefix() {
        String configuredPrefix = properties.keyPrefix();
        return configuredPrefix == null || configuredPrefix.isBlank() ? DEFAULT_KEY_PREFIX : configuredPrefix;
    }

    private Duration franquiciaByIdTtl() {
        return properties.franquiciaByIdTtl() == null ? DEFAULT_FRANQUICIA_BY_ID_TTL : properties.franquiciaByIdTtl();
    }

    private Duration franquiciaAllTtl() {
        return properties.franquiciaAllTtl() == null ? DEFAULT_FRANQUICIA_ALL_TTL : properties.franquiciaAllTtl();
    }
}
