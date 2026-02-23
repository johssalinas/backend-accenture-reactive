package co.com.accenture.model.sucursal;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SucursalTest {

    @Test
    void builderShouldCreateSucursalWithValues() {
        UUID id = UUID.randomUUID();
        UUID franquiciaId = UUID.randomUUID();

        Sucursal sucursal = Sucursal.builder()
                .id(id)
                .name("Sucursal Centro")
                .franquiciaId(franquiciaId)
                .build();

        assertNotNull(sucursal);
        assertEquals(id, sucursal.getId());
        assertEquals("Sucursal Centro", sucursal.getName());
        assertEquals(franquiciaId, sucursal.getFranquiciaId());
    }

    @Test
    void toBuilderShouldCloneAndAllowChanges() {
        UUID id = UUID.randomUUID();
        UUID franquiciaId = UUID.randomUUID();
        Sucursal original = Sucursal.builder()
                .id(id)
                .name("Original")
                .franquiciaId(franquiciaId)
                .build();

        Sucursal changed = original.toBuilder().name("Actualizada").build();

        assertEquals(id, changed.getId());
        assertEquals("Actualizada", changed.getName());
        assertEquals(franquiciaId, changed.getFranquiciaId());
    }
}
