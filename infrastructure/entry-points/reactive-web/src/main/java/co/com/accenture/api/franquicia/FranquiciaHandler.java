package co.com.accenture.api.franquicia;

import co.com.accenture.model.franquicia.Franquicia;
import co.com.accenture.usecase.franquicia.FranquiciaUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FranquiciaHandler {
    private final FranquiciaUseCase useCase;

    public Mono<ServerResponse> save(ServerRequest request) {
        return request.bodyToMono(Franquicia.class)
                .flatMap(useCase::save)
                .flatMap(franquiciaCreada ->
                    ServerResponse.created(URI.create("/api/franquicias/" + franquiciaCreada.getId()))
                                .bodyValue(franquiciaCreada)
                );
    }

    public Mono<ServerResponse> findById(ServerRequest request) {
        UUID id = UUID.fromString(request.pathVariable("id"));
        return useCase.findById(id)
                .flatMap(franquiciaEncontrada -> ServerResponse.ok().bodyValue(franquiciaEncontrada))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> findAll(ServerRequest request) {
        return ServerResponse.ok().body(useCase.findAll(), Franquicia.class);
    }

    public Mono<ServerResponse> deleteById(ServerRequest request) {
        UUID id = UUID.fromString(request.pathVariable("id"));
        return useCase.deleteById(id)
                .then(ServerResponse.noContent().build());
    }

    public Mono<ServerResponse> updateName(ServerRequest request) {
        UUID id = UUID.fromString(request.pathVariable("id"));
        return request.bodyToMono(Franquicia.class)
                .flatMap(body ->
                        ServerResponse.ok()
                                .body(useCase.updateName(id, body.getName()), Franquicia.class)
                );
    }
}
