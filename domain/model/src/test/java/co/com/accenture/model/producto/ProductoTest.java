package co.com.accenture.model.producto;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProductoTest {

    @Test
    void builderShouldCreateProductoWithValues() {
        UUID id = UUID.randomUUID();
        UUID sucursalId = UUID.randomUUID();

        Producto producto = Producto.builder()
                .id(id)
                .name("Producto A")
                .sucursales(List.of(ProductoStock.builder().sucursalId(sucursalId).stock(10).build()))
                .build();

        assertNotNull(producto);
        assertEquals(id, producto.getId());
        assertEquals("Producto A", producto.getName());
        assertEquals(1, producto.getSucursales().size());
        assertEquals(sucursalId, producto.getSucursales().get(0).getSucursalId());
        assertEquals(10, producto.getSucursales().get(0).getStock());
    }

    @Test
    void toBuilderShouldCloneAndAllowChanges() {
        UUID id = UUID.randomUUID();
        Producto original = Producto.builder()
                .id(id)
                .name("Original")
                .sucursales(List.of())
                .build();

        Producto changed = original.toBuilder().name("Actualizado").build();

        assertEquals(id, changed.getId());
        assertEquals("Actualizado", changed.getName());
    }
}
