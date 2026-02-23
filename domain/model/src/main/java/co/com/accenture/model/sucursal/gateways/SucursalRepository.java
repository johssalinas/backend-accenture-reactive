package co.com.accenture.model.sucursal.gateways;

import co.com.accenture.model.sucursal.Sucursal;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface SucursalRepository {
    Mono<Sucursal> save(Sucursal sucursal);

    Mono<Sucursal> findById(UUID id);

    Flux<Sucursal> findAll();

    Mono<Void> deleteById(UUID id);

    Mono<Sucursal> updateName(UUID id, String name);
}
