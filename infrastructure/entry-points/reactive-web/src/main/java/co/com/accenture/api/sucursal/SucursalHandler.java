package co.com.accenture.api.sucursal;

import co.com.accenture.api.config.ApiErrorResponseMapper;
import co.com.accenture.model.exception.BusinessException;
import co.com.accenture.model.exception.TechnicalException;
import co.com.accenture.model.exception.message.BusinessErrorMessage;
import co.com.accenture.model.sucursal.Sucursal;
import co.com.accenture.usecase.sucursal.SucursalUseCase;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SucursalHandler {
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final String CLIENT_ID_HEADER = "X-Client-Id";

    private final SucursalUseCase useCase;

    public Mono<ServerResponse> save(@NonNull ServerRequest request) {
        return request.bodyToMono(Sucursal.class)
                .switchIfEmpty(getBusinessError(BusinessErrorMessage.INVALID_REQUEST_BODY))
                .flatMap(sucursal -> useCase.save(
                        request.headers().firstHeader(CLIENT_ID_HEADER),
                        request.headers().firstHeader(IDEMPOTENCY_KEY_HEADER),
                        sucursal))
                .flatMap(sucursalCreada -> ServerResponse
                        .created(URI.create("/api/sucursales/" + sucursalCreada.getId()))
                        .bodyValue(sucursalCreada))
                .onErrorResume(BusinessException.class, ApiErrorResponseMapper::mapBusiness)
                .onErrorResume(TechnicalException.class, ApiErrorResponseMapper::mapTechnical)
                .onErrorResume(error -> ApiErrorResponseMapper.mapUnknown());
    }

    public Mono<ServerResponse> findById(@NonNull ServerRequest request) {
        return getPathId(request)
                .flatMap(useCase::findById)
                .flatMap(sucursalEncontrada -> ServerResponse.ok().bodyValue(sucursalEncontrada))
                .onErrorResume(BusinessException.class, ApiErrorResponseMapper::mapBusiness)
                .onErrorResume(TechnicalException.class, ApiErrorResponseMapper::mapTechnical)
                .onErrorResume(error -> ApiErrorResponseMapper.mapUnknown());
    }

    public Mono<ServerResponse> findAll(ServerRequest request) {
        return ServerResponse.ok().body(useCase.findAll(), Sucursal.class)
                .onErrorResume(TechnicalException.class, ApiErrorResponseMapper::mapTechnical)
                .onErrorResume(error -> ApiErrorResponseMapper.mapUnknown());
    }

    public Mono<ServerResponse> deleteById(@NonNull ServerRequest request) {
        return getPathId(request)
                .flatMap(useCase::deleteById)
                .then(ServerResponse.noContent().build())
                .onErrorResume(BusinessException.class, ApiErrorResponseMapper::mapBusiness)
                .onErrorResume(TechnicalException.class, ApiErrorResponseMapper::mapTechnical)
                .onErrorResume(error -> ApiErrorResponseMapper.mapUnknown());
    }

    public Mono<ServerResponse> updateName(@NonNull ServerRequest request) {
        return getPathId(request)
                .flatMap(id -> request.bodyToMono(Sucursal.class)
                        .switchIfEmpty(getBusinessError(BusinessErrorMessage.INVALID_REQUEST_BODY))
                        .flatMap(body -> useCase.updateName(id, body.getName())))
                .flatMap(updated -> ServerResponse.ok().bodyValue(updated))
                .onErrorResume(BusinessException.class, ApiErrorResponseMapper::mapBusiness)
                .onErrorResume(TechnicalException.class, ApiErrorResponseMapper::mapTechnical)
                .onErrorResume(error -> ApiErrorResponseMapper.mapUnknown());
    }

    private Mono<UUID> getPathId(ServerRequest request) {
        return Mono.fromCallable(() -> UUID.fromString(request.pathVariable("id")))
                .onErrorResume(IllegalArgumentException.class,
                        error -> getBusinessError(BusinessErrorMessage.INVALID_RESOURCE_ID));
    }

    private <T> Mono<T> getBusinessError(BusinessErrorMessage businessErrorMessage) {
        return Mono.error(new BusinessException(businessErrorMessage));
    }
}
