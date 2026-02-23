package co.com.accenture.redis.idempotency;

import co.com.accenture.model.idempotency.IdempotencyRecord;
import co.com.accenture.model.idempotency.IdempotencyStatus;
import co.com.accenture.model.idempotency.gateways.IdempotencyRepository;
import co.com.accenture.r2dbc.IdempotencyKeyAdapter;
import co.com.accenture.redis.config.RedisConnectionProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

@Primary
@Repository
@RequiredArgsConstructor
public class HybridIdempotencyRepositoryAdapter implements IdempotencyRepository {

    private static final String DEFAULT_KEY_PREFIX = "accenture";
    private static final Duration DEFAULT_IN_PROGRESS_TTL = Duration.ofSeconds(30);
    private static final Duration DEFAULT_COMPLETED_TTL = Duration.ofHours(24);
    private static final String NULL_TOKEN = "~";

    private final ReactiveStringRedisTemplate redisTemplate;
    private final IdempotencyKeyAdapter postgresqlRepository;
    private final RedisConnectionProperties properties;

    @Override
    public Mono<Boolean> tryAcquire(String clientId, String idempotencyKey, String requestHash) {
        return postgresqlRepository.tryAcquire(clientId, idempotencyKey, requestHash)
                .flatMap(acquired -> acquired
                        ? cacheRecord(IdempotencyRecord.builder()
                                .clientId(clientId)
                                .idempotencyKey(idempotencyKey)
                                .requestHash(requestHash)
                                .status(IdempotencyStatus.IN_PROGRESS)
                                .responsePayload(null)
                                .build()).thenReturn(true)
                        : synchronizeRecord(clientId, idempotencyKey).thenReturn(false));
    }

    @Override
    public Mono<IdempotencyRecord> findByClientAndKey(String clientId, String idempotencyKey) {
        return getFromCache(clientId, idempotencyKey)
                .flatMap(cachedRecord -> cachedRecord.getStatus() == IdempotencyStatus.COMPLETED
                        ? Mono.just(cachedRecord)
                        : postgresqlRepository.findByClientAndKey(clientId, idempotencyKey)
                                .flatMap(record -> cacheRecord(record).thenReturn(record))
                                .switchIfEmpty(Mono.just(cachedRecord)))
                .switchIfEmpty(postgresqlRepository.findByClientAndKey(clientId, idempotencyKey)
                        .flatMap(record -> cacheRecord(record).thenReturn(record)));
    }

    @Override
    public Mono<Void> markCompleted(String clientId, String idempotencyKey, String responsePayload) {
        return postgresqlRepository.markCompleted(clientId, idempotencyKey, responsePayload)
                .then(synchronizeRecord(clientId, idempotencyKey));
    }

    @Override
    public Mono<Void> release(String clientId, String idempotencyKey) {
        return postgresqlRepository.release(clientId, idempotencyKey)
                .then(redisTemplate.delete(cacheKey(clientId, idempotencyKey))
                        .then()
                        .onErrorResume(error -> Mono.empty()));
    }

    private Mono<Void> synchronizeRecord(String clientId, String idempotencyKey) {
        return postgresqlRepository.findByClientAndKey(clientId, idempotencyKey)
                .flatMap(this::cacheRecord)
                .onErrorResume(error -> Mono.empty())
                .then();
    }

    private Mono<IdempotencyRecord> getFromCache(String clientId, String idempotencyKey) {
        return redisTemplate.opsForValue()
                .get(cacheKey(clientId, idempotencyKey))
                .flatMap(serialized -> deserialize(clientId, idempotencyKey, serialized))
                .onErrorResume(error -> Mono.empty());
    }

    private Mono<Void> cacheRecord(IdempotencyRecord record) {
        Duration ttl = record.getStatus() == IdempotencyStatus.COMPLETED ? completedTtl() : inProgressTtl();
        return serialize(record)
                .flatMap(serialized -> redisTemplate.opsForValue()
                        .set(cacheKey(record.getClientId(), record.getIdempotencyKey()), serialized, ttl)
                        .then())
                .onErrorResume(error -> Mono.empty());
    }

    private Mono<String> serialize(IdempotencyRecord record) {
        return Mono.fromSupplier(() -> {
            String requestHash = tokenOrValue(record.getRequestHash());
            String status = record.getStatus() == null ? IdempotencyStatus.IN_PROGRESS.name()
                    : record.getStatus().name();
            String responsePayload = record.getResponsePayload() == null
                    ? NULL_TOKEN
                    : Base64.getUrlEncoder()
                            .encodeToString(record.getResponsePayload().getBytes(StandardCharsets.UTF_8));
            return requestHash + "|" + status + "|" + responsePayload;
        });
    }

    private Mono<IdempotencyRecord> deserialize(String clientId, String idempotencyKey, String payload) {
        return Mono.fromSupplier(() -> {
            String[] parts = payload.split("\\|", 3);
            String requestHash = valueOrNull(parts[0]);
            IdempotencyStatus status = IdempotencyStatus.valueOf(parts[1]);
            String responsePayload = NULL_TOKEN.equals(parts[2])
                    ? null
                    : new String(Base64.getUrlDecoder().decode(parts[2]), StandardCharsets.UTF_8);

            return IdempotencyRecord.builder()
                    .clientId(clientId)
                    .idempotencyKey(idempotencyKey)
                    .requestHash(requestHash)
                    .status(status)
                    .responsePayload(responsePayload)
                    .build();
        }).onErrorResume(error -> Mono.empty());
    }

    private String cacheKey(String clientId, String idempotencyKey) {
        return keyPrefix() + ":idempotency:" + clientId + ":" + idempotencyKey;
    }

    private String keyPrefix() {
        String configuredPrefix = properties.keyPrefix();
        return configuredPrefix == null || configuredPrefix.isBlank() ? DEFAULT_KEY_PREFIX : configuredPrefix;
    }

    private Duration inProgressTtl() {
        return properties.idempotencyInProgressTtl() == null
                ? DEFAULT_IN_PROGRESS_TTL
                : properties.idempotencyInProgressTtl();
    }

    private Duration completedTtl() {
        return properties.idempotencyCompletedTtl() == null
                ? DEFAULT_COMPLETED_TTL
                : properties.idempotencyCompletedTtl();
    }

    private static String tokenOrValue(String value) {
        return value == null || value.isBlank() ? NULL_TOKEN : value;
    }

    private static String valueOrNull(String value) {
        return NULL_TOKEN.equals(value) ? null : value;
    }
}
