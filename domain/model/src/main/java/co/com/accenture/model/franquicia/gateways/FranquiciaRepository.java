package co.com.accenture.model.franquicia.gateways;

import co.com.accenture.model.franquicia.Franquicia;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface FranquiciaRepository {
    Mono<Franquicia> save(Franquicia franquicia);
    Mono<Franquicia> findById(UUID id);
    Flux<Franquicia> findAll();
    Mono<Void> deleteById(UUID id);
}
