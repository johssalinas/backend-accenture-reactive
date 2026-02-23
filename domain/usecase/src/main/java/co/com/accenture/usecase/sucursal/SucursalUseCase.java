package co.com.accenture.usecase.sucursal;

import co.com.accenture.model.exception.message.BusinessErrorMessage;
import co.com.accenture.model.franquicia.gateways.FranquiciaRepository;
import co.com.accenture.model.idempotency.gateways.IdempotencyRepository;
import co.com.accenture.model.sucursal.Sucursal;
import co.com.accenture.model.sucursal.gateways.SucursalCacheRepository;
import co.com.accenture.model.sucursal.gateways.SucursalRepository;
import co.com.accenture.model.validation.ReactiveValidationUtils;
import co.com.accenture.usecase.idempotency.IdempotencyFlowHelper;
import co.com.accenture.usecase.sucursal.helper.SucursalIdempotencyCodec;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class SucursalUseCase {

    private final SucursalRepository repository;
    private final SucursalCacheRepository cacheRepository;
    private final FranquiciaRepository franquiciaRepository;
    private final IdempotencyRepository idempotencyRepository;

    public Mono<Sucursal> save(String clientId, String idempotencyKey, Sucursal sucursal) {
        return IdempotencyFlowHelper.execute(
                idempotencyRepository,
                clientId,
                idempotencyKey,
                sucursal,
                () -> ReactiveValidationUtils
                        .requireNonNull(sucursal, BusinessErrorMessage.INVALID_REQUEST_BODY)
                        .flatMap(request -> ReactiveValidationUtils
                                .requireNonBlank(request.getName(), BusinessErrorMessage.INVALID_RESOURCE_NAME)
                                .flatMap(validName -> ReactiveValidationUtils
                                        .requireNonNull(request.getFranquiciaId(),
                                                BusinessErrorMessage.INVALID_PARENT_RESOURCE_ID)
                                        .flatMap(validFranquiciaId -> franquiciaRepository.findById(validFranquiciaId)
                                                .switchIfEmpty(ReactiveValidationUtils
                                                        .businessError(BusinessErrorMessage.RESOURCE_NOT_FOUND))
                                                .thenReturn(request.toBuilder()
                                                        .name(validName)
                                                        .franquiciaId(validFranquiciaId)
                                                        .build()))))
                        .flatMap(repository::save)
                        .flatMap(saved -> cacheRepository.putById(saved)
                                .then(cacheRepository.evictAll())
                                .thenReturn(saved)),
                SucursalIdempotencyCodec.INSTANCE);
    }

    public Mono<Sucursal> findById(UUID id) {
        return ReactiveValidationUtils.requireNonNull(id, BusinessErrorMessage.INVALID_RESOURCE_ID)
                .flatMap(validId -> cacheRepository.getById(validId)
                        .switchIfEmpty(Mono.defer(() -> repository.findById(validId)
                                .flatMap(found -> cacheRepository.putById(found).thenReturn(found)))))
                .switchIfEmpty(ReactiveValidationUtils.businessError(BusinessErrorMessage.RESOURCE_NOT_FOUND));
    }

    public Flux<Sucursal> findAll() {
        return cacheRepository.getAll()
                .switchIfEmpty(Flux.defer(() -> repository.findAll()
                        .collectList()
                        .flatMapMany(found -> found.isEmpty()
                                ? Flux.empty()
                                : cacheRepository.putAll(found).thenMany(Flux.fromIterable(found)))));
    }

    public Mono<Void> deleteById(UUID id) {
        return ReactiveValidationUtils.requireNonNull(id, BusinessErrorMessage.INVALID_RESOURCE_ID)
                .flatMap(repository::findById)
                .switchIfEmpty(ReactiveValidationUtils.businessError(BusinessErrorMessage.RESOURCE_NOT_FOUND))
                .flatMap(sucursal -> repository.deleteById(id)
                        .then(cacheRepository.evictById(id))
                        .then(cacheRepository.evictAll()));
    }

    public Mono<Sucursal> updateName(UUID id, String newName) {
        return ReactiveValidationUtils.requireNonNull(id, BusinessErrorMessage.INVALID_RESOURCE_ID)
                .flatMap(validId -> ReactiveValidationUtils
                        .requireNonBlank(newName, BusinessErrorMessage.INVALID_RESOURCE_NAME)
                        .flatMap(validName -> repository.updateName(validId, validName)
                                .flatMap(updated -> cacheRepository.putById(updated)
                                        .then(cacheRepository.evictAll())
                                        .thenReturn(updated))))
                .switchIfEmpty(ReactiveValidationUtils.businessError(BusinessErrorMessage.RESOURCE_NOT_FOUND));
    }
}
