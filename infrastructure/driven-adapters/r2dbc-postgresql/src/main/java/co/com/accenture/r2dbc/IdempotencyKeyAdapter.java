package co.com.accenture.r2dbc;

import co.com.accenture.model.idempotency.IdempotencyRecord;
import co.com.accenture.model.idempotency.IdempotencyStatus;
import co.com.accenture.model.idempotency.gateways.IdempotencyRepository;
import co.com.accenture.r2dbc.entities.IdempotencyKeyEntity;
import co.com.accenture.r2dbc.helper.DatabaseExceptionMapper;
import co.com.accenture.r2dbc.repository.IdempotencyKeyDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class IdempotencyKeyAdapter implements IdempotencyRepository {

    private final IdempotencyKeyDataRepository repository;

    @Override
    public Mono<Boolean> tryAcquire(String clientId, String idempotencyKey, String requestHash) {
        return repository.tryAcquire(clientId, idempotencyKey, requestHash)
                .map(rows -> rows > 0)
                .onErrorMap(DatabaseExceptionMapper::map);
    }

    @Override
    public Mono<IdempotencyRecord> findByClientAndKey(String clientId, String idempotencyKey) {
        return repository.findByClientIdAndIdempotencyKey(clientId, idempotencyKey)
                .map(this::toDomain)
                .onErrorMap(DatabaseExceptionMapper::map);
    }

    @Override
    public Mono<Void> markCompleted(String clientId, String idempotencyKey, String responsePayload) {
        return repository.markCompleted(clientId, idempotencyKey, responsePayload)
                .then()
                .onErrorMap(DatabaseExceptionMapper::map);
    }

    @Override
    public Mono<Void> release(String clientId, String idempotencyKey) {
        return repository.release(clientId, idempotencyKey)
                .then()
                .onErrorMap(DatabaseExceptionMapper::map);
    }

    private IdempotencyRecord toDomain(IdempotencyKeyEntity entity) {
        return IdempotencyRecord.builder()
                .clientId(entity.getClientId())
                .idempotencyKey(entity.getIdempotencyKey())
                .requestHash(entity.getRequestHash())
                .status(IdempotencyStatus.valueOf(entity.getStatus()))
                .responsePayload(entity.getResponsePayload())
                .build();
    }
}
