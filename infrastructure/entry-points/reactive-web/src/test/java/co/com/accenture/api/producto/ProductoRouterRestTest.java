package co.com.accenture.api.producto;

import co.com.accenture.api.config.CorsConfig;
import co.com.accenture.api.config.SecurityHeadersConfig;
import co.com.accenture.model.exception.BusinessException;
import co.com.accenture.model.exception.message.BusinessErrorMessage;
import co.com.accenture.model.producto.Producto;
import co.com.accenture.model.producto.ProductoMaxStockPorSucursal;
import co.com.accenture.model.producto.ProductoStock;
import co.com.accenture.usecase.producto.ProductoUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = { ProductoRouterRest.class, ProductoHandler.class })
@WebFluxTest
@Import({ CorsConfig.class, SecurityHeadersConfig.class })
class ProductoRouterRestTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ProductoUseCase useCase;

    @Test
    void saveShouldReturnCreated() {
        UUID id = UUID.randomUUID();
        UUID sucursalId = UUID.randomUUID();
        Producto response = Producto.builder()
                .id(id)
                .name("Producto A")
                .sucursales(List.of(ProductoStock.builder().sucursalId(sucursalId).stock(10).build()))
                .build();

        when(useCase.save(eq("cliente-1"), eq("idem-1"), any(Producto.class)))
                .thenReturn(Mono.just(response));

        webTestClient.post()
                .uri("/api/productos")
                .header("X-Client-Id", "cliente-1")
                .header("Idempotency-Key", "idem-1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                                {
                                  "name": "Producto A",
                                  "sucursales": [
                                    {
                                      "sucursalId": "%s",
                                      "stock": 10
                                    }
                                  ]
                                }
                                """.formatted(sucursalId))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().valueEquals("Location", "/api/productos/" + id)
                .expectBody()
                .jsonPath("$.name").isEqualTo("Producto A");
    }

    @Test
    void findByIdShouldReturnBadRequestWhenIdIsInvalid() {
        webTestClient.get()
                .uri("/api/productos/id-invalido")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("BUS4001");
    }

    @Test
    void updateNameShouldReturnNotFound() {
        UUID id = UUID.randomUUID();
        when(useCase.updateName(eq(id), eq("Nuevo Nombre")))
                .thenReturn(Mono.error(
                        new BusinessException(BusinessErrorMessage.RESOURCE_NOT_FOUND)));

        webTestClient.patch()
                .uri("/api/productos/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                                {
                                  "name": "Nuevo Nombre"
                                }
                                """)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("BUS4040");
    }

    @Test
    void maxStockEndpointShouldReturnData() {
        UUID franquiciaId = UUID.randomUUID();
        ProductoMaxStockPorSucursal result = ProductoMaxStockPorSucursal.builder()
                .sucursalId(UUID.randomUUID())
                .sucursalName("Sucursal Centro")
                .productoId(UUID.randomUUID())
                .productoName("Producto Top")
                .stock(100)
                .build();

        when(useCase.findMaxStockBySucursalForFranquicia(franquiciaId)).thenReturn(Flux.just(result));

        webTestClient.get()
                .uri("/api/franquicias/{franquiciaId}/productos/max-stock-por-sucursal", franquiciaId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].sucursalName").isEqualTo("Sucursal Centro")
                .jsonPath("$[0].productoName").isEqualTo("Producto Top")
                .jsonPath("$[0].stock").isEqualTo(100);
    }
}
