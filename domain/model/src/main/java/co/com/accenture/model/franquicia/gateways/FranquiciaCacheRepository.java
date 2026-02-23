package co.com.accenture.model.franquicia.gateways;

import co.com.accenture.model.franquicia.Franquicia;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public interface FranquiciaCacheRepository {
    Mono<Franquicia> getById(UUID id);

    Mono<Void> putById(Franquicia franquicia);

    Flux<Franquicia> getAll();

    Mono<Void> putAll(List<Franquicia> franquicias);

    Mono<Void> evictById(UUID id);

    Mono<Void> evictAll();
}
