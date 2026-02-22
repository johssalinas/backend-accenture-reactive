package co.com.accenture.usecase.idempotency;

import co.com.accenture.model.exception.message.BusinessErrorMessage;
import co.com.accenture.model.idempotency.IdempotencyStatus;
import co.com.accenture.model.idempotency.gateways.IdempotencyRepository;
import co.com.accenture.model.validation.ReactiveValidationUtils;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

public final class IdempotencyFlowHelper {

    private IdempotencyFlowHelper() {
    }

    public static <T> Mono<T> execute(IdempotencyRepository idempotencyRepository,
            String clientId,
            String idempotencyKey,
            T request,
            Supplier<Mono<T>> createOperation,
            IdempotencyPayloadCodec<T> payloadCodec) {
        return execute(
                idempotencyRepository,
                clientId,
                idempotencyKey,
                IdempotencyHashHelper.hash(payloadCodec.serialize(request)),
                createOperation,
                payloadCodec);
    }

    public static <T> Mono<T> execute(IdempotencyRepository idempotencyRepository,
            String clientId,
            String idempotencyKey,
            String requestHash,
            Supplier<Mono<T>> createOperation,
            IdempotencyPayloadCodec<T> payloadCodec) {
        return ReactiveValidationUtils.requireNonBlank(clientId, BusinessErrorMessage.INVALID_CLIENT_ID)
                .zipWith(ReactiveValidationUtils.requireNonBlank(idempotencyKey,
                        BusinessErrorMessage.INVALID_IDEMPOTENCY_KEY))
                .flatMap(validHeaders -> processIdempotentSave(
                        idempotencyRepository,
                        validHeaders.getT1(),
                        validHeaders.getT2(),
                        requestHash,
                        createOperation,
                        payloadCodec));
    }

    public static <T> Mono<T> processIdempotentSave(IdempotencyRepository idempotencyRepository,
            String clientId,
            String idempotencyKey,
            String requestHash,
            Supplier<Mono<T>> createOperation,
            IdempotencyPayloadCodec<T> payloadCodec) {
        return idempotencyRepository.tryAcquire(clientId, idempotencyKey, requestHash)
                .flatMap(acquired -> acquired
                        ? executeCreateAndPersistIdempotency(idempotencyRepository,
                                clientId,
                                idempotencyKey,
                                createOperation,
                                payloadCodec)
                        : resolveExistingRequest(idempotencyRepository,
                                clientId,
                                idempotencyKey,
                                requestHash,
                                payloadCodec));
    }

    public static <T> Mono<T> executeCreateAndPersistIdempotency(IdempotencyRepository idempotencyRepository,
            String clientId,
            String idempotencyKey,
            Supplier<Mono<T>> createOperation,
            IdempotencyPayloadCodec<T> payloadCodec) {
        return createOperation.get()
                .flatMap(saved -> idempotencyRepository
                        .markCompleted(clientId, idempotencyKey, payloadCodec.serialize(saved))
                        .thenReturn(saved))
                .onErrorResume(error -> idempotencyRepository.release(clientId, idempotencyKey)
                        .onErrorResume(releaseError -> Mono.empty())
                        .then(Mono.error(error)));
    }

    public static <T> Mono<T> resolveExistingRequest(IdempotencyRepository idempotencyRepository,
            String clientId,
            String idempotencyKey,
            String requestHash,
            IdempotencyPayloadCodec<T> payloadCodec) {
        return idempotencyRepository.findByClientAndKey(clientId, idempotencyKey)
                .switchIfEmpty(
                        ReactiveValidationUtils.businessError(BusinessErrorMessage.IDEMPOTENCY_REQUEST_IN_PROGRESS))
                .flatMap(record -> IdempotencyConflictHelper.validateRequestHash(record, requestHash)
                        .then(record.getStatus() == IdempotencyStatus.COMPLETED
                                ? payloadCodec.deserialize(record.getResponsePayload())
                                : IdempotencyWaitHelper.waitForCompletedResponse(idempotencyRepository,
                                        clientId,
                                        idempotencyKey,
                                        payloadCodec)));
    }
}