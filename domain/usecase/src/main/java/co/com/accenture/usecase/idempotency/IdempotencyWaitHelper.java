package co.com.accenture.usecase.idempotency;

import co.com.accenture.model.exception.message.BusinessErrorMessage;
import co.com.accenture.model.idempotency.IdempotencyStatus;
import co.com.accenture.model.idempotency.gateways.IdempotencyRepository;
import co.com.accenture.model.validation.ReactiveValidationUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

public final class IdempotencyWaitHelper {

    private static final Duration IDEMPOTENCY_WAIT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration IDEMPOTENCY_POLL_INTERVAL = Duration.ofMillis(100);

    private IdempotencyWaitHelper() {
    }

    public static <T> Mono<T> waitForCompletedResponse(IdempotencyRepository idempotencyRepository,
            String clientId,
            String idempotencyKey,
            IdempotencyPayloadCodec<T> payloadCodec) {
        return Flux.interval(IDEMPOTENCY_POLL_INTERVAL)
                .concatMap(tick -> idempotencyRepository.findByClientAndKey(clientId, idempotencyKey))
                .filter(record -> record.getStatus() == IdempotencyStatus.COMPLETED)
                .concatMap(record -> payloadCodec.deserialize(record.getResponsePayload()))
                .next()
                .timeout(IDEMPOTENCY_WAIT_TIMEOUT)
                .onErrorResume(TimeoutException.class,
                        error -> ReactiveValidationUtils
                                .businessError(BusinessErrorMessage.IDEMPOTENCY_REQUEST_IN_PROGRESS));
    }
}
