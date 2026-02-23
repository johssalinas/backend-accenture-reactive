package co.com.accenture.model.idempotency.gateways;

import co.com.accenture.model.idempotency.IdempotencyRecord;
import reactor.core.publisher.Mono;

public interface IdempotencyRepository {
    Mono<Boolean> tryAcquire(String clientId, String idempotencyKey, String requestHash);

    Mono<IdempotencyRecord> findByClientAndKey(String clientId, String idempotencyKey);

    Mono<Void> markCompleted(String clientId, String idempotencyKey, String responsePayload);

    Mono<Void> release(String clientId, String idempotencyKey);
}
