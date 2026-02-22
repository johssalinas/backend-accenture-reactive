package co.com.accenture.api.franquicia;

import co.com.accenture.api.config.ApiErrorResponseMapper;
import co.com.accenture.model.exception.BusinessException;
import co.com.accenture.model.exception.TechnicalException;
import co.com.accenture.model.exception.message.BusinessErrorMessage;
import co.com.accenture.model.franquicia.Franquicia;
import co.com.accenture.usecase.franquicia.FranquiciaUseCase;
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
public class FranquiciaHandler {
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final String CLIENT_ID_HEADER = "X-Client-Id";

    private final FranquiciaUseCase useCase;

    public Mono<ServerResponse> save(@NonNull ServerRequest request) {
        return request.bodyToMono(Franquicia.class)
                .switchIfEmpty(getBusinessError(BusinessErrorMessage.INVALID_FRANQUICIA_REQUEST))
                .flatMap(franquicia -> useCase.save(
                        request.headers().firstHeader(CLIENT_ID_HEADER),
                        request.headers().firstHeader(IDEMPOTENCY_KEY_HEADER),
                        franquicia))
                .flatMap(franquiciaCreada -> ServerResponse
                        .created(URI.create("/api/franquicias/" + franquiciaCreada.getId()))
                        .bodyValue(franquiciaCreada))
                .onErrorResume(BusinessException.class, ApiErrorResponseMapper::mapBusiness)
                .onErrorResume(TechnicalException.class, ApiErrorResponseMapper::mapTechnical)
                .onErrorResume(error -> ApiErrorResponseMapper.mapUnknown());
    }

    public Mono<ServerResponse> findById(@NonNull ServerRequest request) {
        return getPathId(request)
                .flatMap(useCase::findById)
                .flatMap(franquiciaEncontrada -> ServerResponse.ok().bodyValue(franquiciaEncontrada))
                .onErrorResume(BusinessException.class, ApiErrorResponseMapper::mapBusiness)
                .onErrorResume(TechnicalException.class, ApiErrorResponseMapper::mapTechnical)
                .onErrorResume(error -> ApiErrorResponseMapper.mapUnknown());
    }

    public Mono<ServerResponse> findAll(ServerRequest request) {
        return ServerResponse.ok().body(useCase.findAll(), Franquicia.class)
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
                .flatMap(id -> request.bodyToMono(Franquicia.class)
                        .switchIfEmpty(getBusinessError(BusinessErrorMessage.INVALID_FRANQUICIA_REQUEST))
                        .flatMap(body -> useCase.updateName(id, body.getName())))
                .flatMap(updated -> ServerResponse.ok().bodyValue(updated))
                .onErrorResume(BusinessException.class, ApiErrorResponseMapper::mapBusiness)
                .onErrorResume(TechnicalException.class, ApiErrorResponseMapper::mapTechnical)
                .onErrorResume(error -> ApiErrorResponseMapper.mapUnknown());
    }

    private Mono<UUID> getPathId(ServerRequest request) {
        return Mono.fromCallable(() -> UUID.fromString(request.pathVariable("id")))
                .onErrorResume(IllegalArgumentException.class,
                        error -> getBusinessError(BusinessErrorMessage.INVALID_FRANQUICIA_ID));
    }

    private <T> Mono<T> getBusinessError(BusinessErrorMessage businessErrorMessage) {
        return Mono.error(new BusinessException(businessErrorMessage));
    }
}
