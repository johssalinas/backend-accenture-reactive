package co.com.accenture.r2dbc.repository;

import co.com.accenture.model.producto.ProductoMaxStockPorSucursal;
import co.com.accenture.model.producto.ProductoStock;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public class ProductoQueryRepository {

    private final DatabaseClient databaseClient;

    public ProductoQueryRepository(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    public Flux<ProductoStock> findStocksByProductoId(UUID productoId) {
        return databaseClient.sql("""
                SELECT sp.sucursal_id,
                       sp.stock
                FROM sucursal_producto sp
                WHERE sp.producto_id = :productoId
                ORDER BY sp.sucursal_id
                """)
                .bind("productoId", productoId)
                .map((row, rowMetadata) -> ProductoStock.builder()
                        .sucursalId(row.get("sucursal_id", UUID.class))
                        .stock(row.get("stock", Integer.class))
                        .build())
                .all();
    }

    public Flux<ProductoMaxStockPorSucursal> findMaxStockBySucursalForFranquicia(UUID franquiciaId) {
        return databaseClient.sql("""
                SELECT s.id AS sucursal_id,
                       s.name AS sucursal_name,
                       p.id AS producto_id,
                       p.name AS producto_name,
                       top_stock.stock AS stock
                FROM sucursal s
                JOIN LATERAL (
                    SELECT sp.producto_id, sp.stock
                    FROM sucursal_producto sp
                    WHERE sp.sucursal_id = s.id
                    ORDER BY sp.stock DESC, sp.producto_id
                    LIMIT 1
                ) top_stock ON TRUE
                JOIN producto p ON p.id = top_stock.producto_id
                WHERE s.franquicia_id = :franquiciaId
                ORDER BY s.name
                """)
                .bind("franquiciaId", franquiciaId)
                .map((row, rowMetadata) -> ProductoMaxStockPorSucursal.builder()
                        .sucursalId(row.get("sucursal_id", UUID.class))
                        .sucursalName(row.get("sucursal_name", String.class))
                        .productoId(row.get("producto_id", UUID.class))
                        .productoName(row.get("producto_name", String.class))
                        .stock(row.get("stock", Integer.class))
                        .build())
                .all();
    }
}
