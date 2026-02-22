package co.com.accenture.r2dbc.repository;

import co.com.accenture.r2dbc.entities.IdempotencyKeyEntity;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface IdempotencyKeyDataRepository extends ReactiveCrudRepository<IdempotencyKeyEntity, UUID> {

    @Modifying
    @Query("""
            INSERT INTO idempotency_key (id, client_id, idempotency_key, request_hash, status)
            VALUES (gen_random_uuid(), :clientId, :idempotencyKey, :requestHash, 'IN_PROGRESS')
            ON CONFLICT (client_id, idempotency_key) DO NOTHING
            """)
    Mono<Integer> tryAcquire(@Param("clientId") String clientId,
            @Param("idempotencyKey") String idempotencyKey,
            @Param("requestHash") String requestHash);

    Mono<IdempotencyKeyEntity> findByClientIdAndIdempotencyKey(String clientId, String idempotencyKey);

    @Modifying
    @Query("""
            UPDATE idempotency_key
               SET status = 'COMPLETED',
                   response_payload = :responsePayload,
                   updated_at = NOW()
             WHERE client_id = :clientId
               AND idempotency_key = :idempotencyKey
            """)
    Mono<Integer> markCompleted(@Param("clientId") String clientId,
            @Param("idempotencyKey") String idempotencyKey,
            @Param("responsePayload") String responsePayload);

    @Modifying
    @Query("""
            DELETE FROM idempotency_key
             WHERE client_id = :clientId
               AND idempotency_key = :idempotencyKey
            """)
    Mono<Integer> release(@Param("clientId") String clientId,
            @Param("idempotencyKey") String idempotencyKey);
}
