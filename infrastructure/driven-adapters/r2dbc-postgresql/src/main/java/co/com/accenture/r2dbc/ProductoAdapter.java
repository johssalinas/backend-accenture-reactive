package co.com.accenture.r2dbc;

import co.com.accenture.model.producto.Producto;
import co.com.accenture.model.producto.ProductoMaxStockPorSucursal;
import co.com.accenture.model.producto.ProductoStock;
import co.com.accenture.model.producto.gateways.ProductoRepository;
import co.com.accenture.r2dbc.entities.ProductoEntity;
import co.com.accenture.r2dbc.helper.DatabaseExceptionMapper;
import co.com.accenture.r2dbc.repository.ProductoDataRepository;
import co.com.accenture.r2dbc.repository.ProductoQueryRepository;
import co.com.accenture.r2dbc.repository.ProductoSucursalDataRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Repository
public class ProductoAdapter implements ProductoRepository {

        private final ProductoDataRepository productoDataRepository;
        private final ProductoSucursalDataRepository productoSucursalDataRepository;
        private final ProductoQueryRepository productoQueryRepository;

        public ProductoAdapter(ProductoDataRepository productoDataRepository,
                        ProductoSucursalDataRepository productoSucursalDataRepository,
                        ProductoQueryRepository productoQueryRepository) {
                this.productoDataRepository = productoDataRepository;
                this.productoSucursalDataRepository = productoSucursalDataRepository;
                this.productoQueryRepository = productoQueryRepository;
        }

        @Override
        @Transactional
        public Mono<Producto> save(Producto producto) {
                ProductoEntity entity = toEntity(producto);
                return productoDataRepository.save(entity)
                                .flatMap(savedEntity -> replaceStocks(savedEntity.getId(), producto.getSucursales())
                                                .then(findById(savedEntity.getId())))
                                .onErrorMap(DatabaseExceptionMapper::map);
        }

        @Override
        public Mono<Producto> findById(UUID id) {
                return productoDataRepository.findById(id)
                                .flatMap(this::toModel)
                                .onErrorMap(DatabaseExceptionMapper::map);
        }

        @Override
        public Flux<Producto> findAll() {
                return productoDataRepository.findAll()
                                .flatMap(this::toModel)
                                .onErrorMap(DatabaseExceptionMapper::map);
        }

        @Override
        @Transactional
        public Mono<Void> deleteById(UUID id) {
                return productoDataRepository.deleteById(id)
                                .onErrorMap(DatabaseExceptionMapper::map);
        }

        @Override
        @Transactional
        public Mono<Producto> updateName(UUID id, String name) {
                return productoDataRepository.updateNameById(id, name)
                                .filter(rowsUpdated -> rowsUpdated > 0)
                                .flatMap(rowsUpdated -> findById(id))
                                .onErrorMap(DatabaseExceptionMapper::map);
        }

        @Override
        public Flux<ProductoMaxStockPorSucursal> findMaxStockBySucursalForFranquicia(UUID franquiciaId) {
                return productoQueryRepository.findMaxStockBySucursalForFranquicia(franquiciaId)
                                .onErrorMap(DatabaseExceptionMapper::map);
        }

        private Mono<Producto> toModel(ProductoEntity entity) {
                return productoQueryRepository.findStocksByProductoId(entity.getId())
                                .collectList()
                                .map(stocks -> Producto.builder()
                                                .id(entity.getId())
                                                .name(entity.getName())
                                                .sucursales(stocks)
                                                .build());
        }

        private ProductoEntity toEntity(Producto producto) {
                return ProductoEntity.builder()
                                .id(producto.getId())
                                .name(producto.getName())
                                .build();
        }

        private Mono<Void> replaceStocks(UUID productoId, List<ProductoStock> sucursales) {
                List<ProductoStock> safeList = sucursales == null ? Collections.emptyList() : sucursales;
                return productoSucursalDataRepository.deleteByProductoId(productoId)
                                .thenMany(Flux.fromIterable(safeList)
                                                .concatMap(stock -> productoSucursalDataRepository.upsertStock(
                                                                stock.getSucursalId(),
                                                                productoId,
                                                                stock.getStock())))
                                .then();
        }

}
