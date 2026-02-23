package co.com.accenture.model.producto.gateways;

import co.com.accenture.model.producto.Producto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public interface ProductoCacheRepository {
    Mono<Producto> getById(UUID id);

    Mono<Void> putById(Producto producto);

    Flux<Producto> getAll();

    Mono<Void> putAll(List<Producto> productos);

    Mono<Void> evictById(UUID id);

    Mono<Void> evictAll();
}
