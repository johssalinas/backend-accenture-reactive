package co.com.accenture.api.franquicia;

import co.com.accenture.model.franquicia.Franquicia;
import co.com.accenture.usecase.franquicia.FranquiciaUseCase;
import io.swagger.v3.oas.models.servers.Server;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.net.URI;

@Component
@RequiredArgsConstructor
public class FranquiciaHandler {
    private final FranquiciaUseCase useCase;

    public Mono<ServerResponse> save(ServerRequest request) {
        return request.bodyToMono(Franquicia.class)
                .flatMap(useCase::save)
                .flatMap(franquiciaCreada ->
                        ServerResponse.created(URI.create("/api/franquicias" + franquiciaCreada.getId()))
                                .bodyValue(franquiciaCreada)
                );
    }

    public Mono<ServerResponse> findById(ServerRequest request) {
        String id = request.pathVariable("id");
        return useCase.findById(id)
                .flatMap(franquiciaEncontrada -> ServerResponse.ok().bodyValue(franquiciaEncontrada))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> findAll(ServerRequest request) {
        return ServerResponse.ok().body(useCase.findAll(), Franquicia.class);
    }

    public Mono<ServerResponse> deleteById(ServerRequest request) {
        String id = request.pathVariable("id");
        return ServerResponse.ok().body(useCase.deleteById(id), Franquicia.class);
    }

    public Mono<ServerResponse> updateName(ServerRequest request) {
        String id = request.pathVariable("id");
        return request.bodyToMono(Franquicia.class)
                .flatMap(body ->
                        ServerResponse.ok()
                                .body(useCase.updateName(body.getName(), id), Franquicia.class)
                );
    }
}
