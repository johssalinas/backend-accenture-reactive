package co.com.accenture.model.producto.gateways;

import co.com.accenture.model.producto.Producto;
import co.com.accenture.model.producto.ProductoMaxStockPorSucursal;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ProductoRepository {
    Mono<Producto> save(Producto producto);

    Mono<Producto> findById(UUID id);

    Flux<Producto> findAll();

    Mono<Void> deleteById(UUID id);

    Mono<Producto> updateName(UUID id, String name);

    Flux<ProductoMaxStockPorSucursal> findMaxStockBySucursalForFranquicia(UUID franquiciaId);
}
