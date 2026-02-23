package co.com.accenture.model.producto;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProductoMaxStockPorSucursalTest {

    @Test
    void builderShouldCreateProjectionWithValues() {
        UUID sucursalId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();

        ProductoMaxStockPorSucursal result = ProductoMaxStockPorSucursal.builder()
                .sucursalId(sucursalId)
                .sucursalName("Sucursal Centro")
                .productoId(productoId)
                .productoName("Producto A")
                .stock(99)
                .build();

        assertNotNull(result);
        assertEquals(sucursalId, result.getSucursalId());
        assertEquals("Sucursal Centro", result.getSucursalName());
        assertEquals(productoId, result.getProductoId());
        assertEquals("Producto A", result.getProductoName());
        assertEquals(99, result.getStock());
    }
}
