package co.com.accenture.r2dbc.repository;

import co.com.accenture.r2dbc.entities.ProductoEntity;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ProductoSucursalDataRepository extends Repository<ProductoEntity, UUID> {

        @Modifying
        @Query("""
                        INSERT INTO sucursal_producto (sucursal_id, producto_id, stock)
                        VALUES (CAST(:sucursalId AS UUID), CAST(:productoId AS UUID), :stock)
                        ON CONFLICT (sucursal_id, producto_id)
                        DO UPDATE SET stock = EXCLUDED.stock
                        """)
        Mono<Integer> upsertStock(@Param("sucursalId") UUID sucursalId,
                        @Param("productoId") UUID productoId,
                        @Param("stock") Integer stock);

        @Modifying
        @Query("DELETE FROM sucursal_producto WHERE producto_id = CAST(:productoId AS UUID)")
        Mono<Integer> deleteByProductoId(@Param("productoId") UUID productoId);
}
