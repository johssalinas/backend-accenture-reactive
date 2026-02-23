package co.com.accenture.usecase.producto;

import co.com.accenture.model.exception.message.BusinessErrorMessage;
import co.com.accenture.model.franquicia.gateways.FranquiciaRepository;
import co.com.accenture.model.idempotency.gateways.IdempotencyRepository;
import co.com.accenture.model.producto.Producto;
import co.com.accenture.model.producto.ProductoMaxStockPorSucursal;
import co.com.accenture.model.producto.ProductoStock;
import co.com.accenture.model.producto.gateways.ProductoCacheRepository;
import co.com.accenture.model.producto.gateways.ProductoRepository;
import co.com.accenture.model.sucursal.gateways.SucursalRepository;
import co.com.accenture.model.validation.ReactiveValidationUtils;
import co.com.accenture.usecase.idempotency.IdempotencyFlowHelper;
import co.com.accenture.usecase.producto.helper.ProductoIdempotencyCodec;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class ProductoUseCase {

    private final ProductoRepository repository;
    private final ProductoCacheRepository cacheRepository;
    private final SucursalRepository sucursalRepository;
    private final FranquiciaRepository franquiciaRepository;
    private final IdempotencyRepository idempotencyRepository;

    public Mono<Producto> save(String clientId, String idempotencyKey, Producto producto) {
        return IdempotencyFlowHelper.execute(
                idempotencyRepository,
                clientId,
                idempotencyKey,
                producto,
                () -> ReactiveValidationUtils
                        .requireNonNull(producto, BusinessErrorMessage.INVALID_REQUEST_BODY)
                        .map(request -> request.getId() == null
                                ? request.toBuilder().id(UUID.randomUUID()).build()
                                : request)
                        .flatMap(this::validateAndNormalize)
                        .flatMap(repository::save)
                        .flatMap(saved -> cacheRepository.putById(saved)
                                .then(cacheRepository.evictAll())
                                .thenReturn(saved)),
                ProductoIdempotencyCodec.INSTANCE);
    }

    public Mono<Producto> findById(UUID id) {
        return ReactiveValidationUtils.requireNonNull(id, BusinessErrorMessage.INVALID_RESOURCE_ID)
                .flatMap(validId -> cacheRepository.getById(validId)
                        .switchIfEmpty(Mono.defer(() -> repository.findById(validId)
                                .flatMap(found -> cacheRepository.putById(found).thenReturn(found)))))
                .switchIfEmpty(ReactiveValidationUtils.businessError(BusinessErrorMessage.RESOURCE_NOT_FOUND));
    }

    public Flux<Producto> findAll() {
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
                .flatMap(producto -> repository.deleteById(id)
                        .then(cacheRepository.evictById(id))
                        .then(cacheRepository.evictAll()));
    }

    public Mono<Producto> updateName(UUID id, String newName) {
        return ReactiveValidationUtils.requireNonNull(id, BusinessErrorMessage.INVALID_RESOURCE_ID)
                .flatMap(validId -> ReactiveValidationUtils
                        .requireNonBlank(newName, BusinessErrorMessage.INVALID_RESOURCE_NAME)
                        .flatMap(validName -> repository.updateName(validId, validName)
                                .flatMap(updated -> cacheRepository.putById(updated)
                                        .then(cacheRepository.evictAll())
                                        .thenReturn(updated))))
                .switchIfEmpty(ReactiveValidationUtils.businessError(BusinessErrorMessage.RESOURCE_NOT_FOUND));
    }

    public Flux<ProductoMaxStockPorSucursal> findMaxStockBySucursalForFranquicia(UUID franquiciaId) {
        return ReactiveValidationUtils.requireNonNull(franquiciaId, BusinessErrorMessage.INVALID_PARENT_RESOURCE_ID)
                .flatMap(validFranquiciaId -> franquiciaRepository.findById(validFranquiciaId)
                        .switchIfEmpty(ReactiveValidationUtils.businessError(BusinessErrorMessage.RESOURCE_NOT_FOUND))
                        .thenReturn(validFranquiciaId))
                .flatMapMany(repository::findMaxStockBySucursalForFranquicia);
    }

    private Mono<Producto> validateAndNormalize(Producto request) {
        return ReactiveValidationUtils.requireNonBlank(request.getName(), BusinessErrorMessage.INVALID_RESOURCE_NAME)
                .flatMap(validName -> validateSucursales(request.getSucursales())
                        .map(validSucursales -> request.toBuilder()
                                .name(validName)
                                .sucursales(validSucursales)
                                .build()));
    }

    private Mono<List<ProductoStock>> validateSucursales(List<ProductoStock> sucursales) {
        return Mono.justOrEmpty(sucursales)
                .filter(list -> !list.isEmpty())
                .switchIfEmpty(ReactiveValidationUtils.businessError(BusinessErrorMessage.INVALID_REQUEST_BODY))
                .flatMapMany(Flux::fromIterable)
                .concatMap(this::validateProductoStock)
                .collectList()
                .map(validList -> validList.stream()
                        .sorted(Comparator.comparing(value -> String.valueOf(value.getSucursalId())))
                        .toList());
    }

    private Mono<ProductoStock> validateProductoStock(ProductoStock productoStock) {
        return ReactiveValidationUtils.requireNonNull(productoStock, BusinessErrorMessage.INVALID_REQUEST_BODY)
                .flatMap(item -> ReactiveValidationUtils
                        .requireNonNull(item.getSucursalId(), BusinessErrorMessage.INVALID_PARENT_RESOURCE_ID)
                        .flatMap(validSucursalId -> validateStock(item.getStock())
                                .flatMap(validStock -> sucursalRepository.findById(validSucursalId)
                                        .switchIfEmpty(ReactiveValidationUtils
                                                .businessError(BusinessErrorMessage.RESOURCE_NOT_FOUND))
                                        .thenReturn(ProductoStock.builder()
                                                .sucursalId(validSucursalId)
                                                .stock(validStock)
                                                .build()))));
    }

    private Mono<Integer> validateStock(Integer stock) {
        return Mono.justOrEmpty(stock)
                .filter(value -> value >= 0)
                .switchIfEmpty(ReactiveValidationUtils.businessError(BusinessErrorMessage.INVALID_RESOURCE_STOCK));
    }
}
