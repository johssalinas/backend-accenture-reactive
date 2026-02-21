package co.com.accenture.usecase.franquicia;

import co.com.accenture.model.franquicia.Franquicia;
import co.com.accenture.model.franquicia.gateways.FranquiciaRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class FranquiciaUseCase {

    private final FranquiciaRepository repository;

    public Mono<Franquicia> save(Franquicia franquicia) {
        return repository.save(franquicia);
    }

    public Mono<Franquicia> findById(String id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Franquicia not found!")));
    }

    public Flux<Franquicia> findAll() {
        return repository.findAll();
    }

    public Mono<Void> deleteById(String id) {
        return repository.deleteById(id);
    }

    public Mono<Franquicia> updateName(String id, String newName) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Not found")))
                .map(franquiciaExistente -> franquiciaExistente.toBuilder().name(newName).build())
                .flatMap(repository::save);
    }
}
