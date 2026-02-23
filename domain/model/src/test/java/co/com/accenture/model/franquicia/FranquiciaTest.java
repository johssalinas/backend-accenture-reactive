package co.com.accenture.model.franquicia;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FranquiciaTest {

    @Test
    void builderShouldCreateFranquiciaWithValues() {
        UUID id = UUID.randomUUID();

        Franquicia franquicia = Franquicia.builder()
                .id(id)
                .name("Franquicia Centro")
                .build();

        assertNotNull(franquicia);
        assertEquals(id, franquicia.getId());
        assertEquals("Franquicia Centro", franquicia.getName());
    }

    @Test
    void toBuilderShouldCloneAndAllowChanges() {
        UUID id = UUID.randomUUID();
        Franquicia original = Franquicia.builder()
                .id(id)
                .name("Original")
                .build();

        Franquicia changed = original.toBuilder().name("Actualizada").build();

        assertEquals(id, changed.getId());
        assertEquals("Actualizada", changed.getName());
    }
}
