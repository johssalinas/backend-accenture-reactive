package co.com.accenture.usecase.franquicia;

import co.com.accenture.model.exception.message.BusinessErrorMessage;
import co.com.accenture.model.franquicia.Franquicia;
import co.com.accenture.model.franquicia.gateways.FranquiciaRepository;
import co.com.accenture.model.idempotency.gateways.IdempotencyRepository;
import co.com.accenture.model.validation.ReactiveValidationUtils;
import co.com.accenture.usecase.franquicia.helper.FranquiciaIdempotencyCodec;
import co.com.accenture.usecase.idempotency.IdempotencyFlowHelper;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class FranquiciaUseCase {

    private final FranquiciaRepository repository;
    private final IdempotencyRepository idempotencyRepository;

    public Mono<Franquicia> save(String clientId, String idempotencyKey, Franquicia franquicia) {
        return ReactiveValidationUtils.requireNonNull(franquicia, BusinessErrorMessage.INVALID_FRANQUICIA_REQUEST)
                .flatMap(request -> ReactiveValidationUtils
                        .requireNonBlank(request.getName(), BusinessErrorMessage.INVALID_FRANQUICIA_NAME)
                        .map(validName -> request.toBuilder().name(validName).build()))
                .flatMap(validRequest -> IdempotencyFlowHelper.execute(
                        idempotencyRepository,
                        clientId,
                        idempotencyKey,
                        validRequest,
                        () -> Mono.just(validRequest.toBuilder().id(UUID.randomUUID()).build())
                                .flatMap(repository::save),
                        FranquiciaIdempotencyCodec.INSTANCE));
    }

    public Mono<Franquicia> findById(UUID id) {
        return ReactiveValidationUtils.requireNonNull(id, BusinessErrorMessage.INVALID_FRANQUICIA_ID)
                .flatMap(repository::findById)
                .switchIfEmpty(ReactiveValidationUtils.businessError(BusinessErrorMessage.FRANQUICIA_NOT_FOUND));
    }

    public Flux<Franquicia> findAll() {
        return repository.findAll();
    }

    public Mono<Void> deleteById(UUID id) {
        return ReactiveValidationUtils.requireNonNull(id, BusinessErrorMessage.INVALID_FRANQUICIA_ID)
                .flatMap(repository::findById)
                .switchIfEmpty(ReactiveValidationUtils.businessError(BusinessErrorMessage.FRANQUICIA_NOT_FOUND))
                .flatMap(franquicia -> repository.deleteById(id));
    }

    public Mono<Franquicia> updateName(UUID id, String newName) {
        return ReactiveValidationUtils.requireNonNull(id, BusinessErrorMessage.INVALID_FRANQUICIA_ID)
                .flatMap(validId -> ReactiveValidationUtils
                        .requireNonBlank(newName, BusinessErrorMessage.INVALID_FRANQUICIA_NAME)
                        .flatMap(validName -> repository.updateName(validId, validName)))
                .switchIfEmpty(ReactiveValidationUtils.businessError(BusinessErrorMessage.FRANQUICIA_NOT_FOUND));
    }
}
