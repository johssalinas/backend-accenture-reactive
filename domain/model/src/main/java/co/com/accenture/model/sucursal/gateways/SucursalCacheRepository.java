package co.com.accenture.model.sucursal.gateways;

import co.com.accenture.model.sucursal.Sucursal;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public interface SucursalCacheRepository {
    Mono<Sucursal> getById(UUID id);

    Mono<Void> putById(Sucursal sucursal);

    Flux<Sucursal> getAll();

    Mono<Void> putAll(List<Sucursal> sucursales);

    Mono<Void> evictById(UUID id);

    Mono<Void> evictAll();
}
