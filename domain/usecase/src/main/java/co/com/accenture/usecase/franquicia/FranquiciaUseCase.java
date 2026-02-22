package co.com.accenture.usecase.franquicia;

import co.com.accenture.model.franquicia.Franquicia;
import co.com.accenture.model.franquicia.gateways.FranquiciaRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class FranquiciaUseCase {

    private final FranquiciaRepository repository;

    public Mono<Franquicia> save(Franquicia franquicia) {
        Franquicia franquiciaToSave = franquicia.toBuilder()
                .id(UUID.randomUUID())
                .build();
        return repository.save(franquiciaToSave);
    }

    public Mono<Franquicia> findById(UUID id) {
        return repository.findById(id);
    }

    public Flux<Franquicia> findAll() {
        return repository.findAll();
    }

    public Mono<Void> deleteById(UUID id) {
        return repository.deleteById(id);
    }

    public Mono<Franquicia> updateName(UUID id, String newName) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Not found")))
                .map(franquiciaExistente -> franquiciaExistente.toBuilder().name(newName).build())
                .flatMap(repository::save);
    }
}
