package co.com.accenture.model.producto;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProductoStockTest {

    @Test
    void builderShouldCreateStockWithValues() {
        UUID sucursalId = UUID.randomUUID();

        ProductoStock stock = ProductoStock.builder()
                .sucursalId(sucursalId)
                .stock(25)
                .build();

        assertNotNull(stock);
        assertEquals(sucursalId, stock.getSucursalId());
        assertEquals(25, stock.getStock());
    }
}
