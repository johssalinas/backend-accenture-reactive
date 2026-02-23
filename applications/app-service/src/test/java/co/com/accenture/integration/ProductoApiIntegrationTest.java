package co.com.accenture.integration;

import co.com.accenture.MainApplication;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

@Testcontainers
@SpringBootTest(classes = MainApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductoApiIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("accenture_db")
            .withUsername("postgres")
            .withPassword("postgres");

    private WebTestClient webTestClient;

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUpClient() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("DB_HOST", postgres::getHost);
        registry.add("DB_PORT", () -> postgres.getMappedPort(5432));
        registry.add("DB_NAME", postgres::getDatabaseName);
        registry.add("DB_USER", postgres::getUsername);
        registry.add("DB_PASSWORD", postgres::getPassword);
        registry.add("JDBC_URL", postgres::getJdbcUrl);
    }

    @Test
    void fullCrudFlowShouldWorkAgainstRealDatabase() {
        UUID franquiciaId = createFranquicia("Franquicia Productos", "create-franquicia-producto-crud");
        UUID sucursalId = createSucursal("Sucursal Producto", franquiciaId, "create-sucursal-producto-crud");

        String createdResponse = webTestClient.post()
                .uri("/api/productos")
                .header("Content-Type", "application/json")
                .header("X-Client-Id", "cliente-producto-crud")
                .header("Idempotency-Key", "crud-producto-001")
                .bodyValue("""
                                {
                                  "name": "Producto Real",
                                  "sucursales": [
                                    {
                                      "sucursalId": "%s",
                                      "stock": 55
                                    }
                                  ]
                                }
                                """.formatted(sucursalId))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        UUID id = UUID.fromString(JsonPath.read(createdResponse, "$.id"));

        webTestClient.get()
                .uri("/api/productos/{id}", id)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(id.toString())
                .jsonPath("$.name").isEqualTo("Producto Real")
                .jsonPath("$.sucursales[0].sucursalId").isEqualTo(sucursalId.toString())
                .jsonPath("$.sucursales[0].stock").isEqualTo(55);

        webTestClient.patch()
                .uri("/api/productos/{id}", id)
                .header("Content-Type", "application/json")
                .bodyValue("""
                                {
                                  "name": "Producto Actualizado"
                                }
                                """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.name").isEqualTo("Producto Actualizado");

        webTestClient.delete()
                .uri("/api/productos/{id}", id)
                .exchange()
                .expectStatus().isNoContent();

        webTestClient.get()
                .uri("/api/productos/{id}", id)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("BUS4040");
    }

    @Test
    void saveShouldBeIdempotentForSameClientAndSameKey() {
        UUID franquiciaId = createFranquicia("Franquicia Idempotente Producto", "create-franquicia-producto-idem");
        UUID sucursalId = createSucursal("Sucursal Idempotente Producto", franquiciaId, "create-sucursal-producto-idem");

        String firstResponse = webTestClient.post()
                .uri("/api/productos")
                .header("Content-Type", "application/json")
                .header("X-Client-Id", "cliente-producto-idempotente")
                .header("Idempotency-Key", "save-producto-001")
                .bodyValue("""
                                {
                                  "name": "Producto Idempotente",
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
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        String secondResponse = webTestClient.post()
                .uri("/api/productos")
                .header("Content-Type", "application/json")
                .header("X-Client-Id", "cliente-producto-idempotente")
                .header("Idempotency-Key", "save-producto-001")
                .bodyValue("""
                                {
                                  "name": "Producto Idempotente",
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
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        UUID firstId = UUID.fromString(JsonPath.read(firstResponse, "$.id"));
        UUID secondId = UUID.fromString(JsonPath.read(secondResponse, "$.id"));

        webTestClient.post()
                .uri("/api/productos")
                .header("Content-Type", "application/json")
                .header("X-Client-Id", "cliente-producto-idempotente")
                .header("Idempotency-Key", "save-producto-001")
                .bodyValue("""
                                {
                                  "name": "Producto Distinto",
                                  "sucursales": [
                                    {
                                      "sucursalId": "%s",
                                      "stock": 11
                                    }
                                  ]
                                }
                                """.formatted(sucursalId))
                .exchange()
                .expectStatus().is4xxClientError()
                .expectBody()
                .jsonPath("$.code").isEqualTo("BUS4091");

        Assertions.assertEquals(firstId, secondId);
    }

    @Test
    void maxStockBySucursalShouldReturnTopProductPerSucursalForFranquicia() {
        UUID franquiciaId = createFranquicia("Franquicia Stock", "create-franquicia-stock-001");
        UUID sucursalNorteId = createSucursal("Sucursal Norte", franquiciaId, "create-sucursal-norte-stock-001");
        UUID sucursalSurId = createSucursal("Sucursal Sur", franquiciaId, "create-sucursal-sur-stock-001");

        webTestClient.post()
                .uri("/api/productos")
                .header("Content-Type", "application/json")
                .header("X-Client-Id", "cliente-stock")
                .header("Idempotency-Key", "create-producto-stock-001")
                .bodyValue("""
                                {
                                  "name": "Producto Uno",
                                  "sucursales": [
                                    {
                                      "sucursalId": "%s",
                                      "stock": 15
                                    },
                                    {
                                      "sucursalId": "%s",
                                      "stock": 30
                                    }
                                  ]
                                }
                                """.formatted(sucursalNorteId, sucursalSurId))
                .exchange()
                .expectStatus().isCreated();

        webTestClient.post()
                .uri("/api/productos")
                .header("Content-Type", "application/json")
                .header("X-Client-Id", "cliente-stock")
                .header("Idempotency-Key", "create-producto-stock-002")
                .bodyValue("""
                                {
                                  "name": "Producto Dos",
                                  "sucursales": [
                                    {
                                      "sucursalId": "%s",
                                      "stock": 20
                                    }
                                  ]
                                }
                                """.formatted(sucursalNorteId))
                .exchange()
                .expectStatus().isCreated();

        String response = webTestClient.get()
                .uri("/api/franquicias/{franquiciaId}/productos/max-stock-por-sucursal", franquiciaId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertEquals("Sucursal Norte", JsonPath.read(response, "$[0].sucursalName"));
        Assertions.assertEquals("Producto Dos", JsonPath.read(response, "$[0].productoName"));
        Assertions.assertEquals(20, ((Number) JsonPath.read(response, "$[0].stock")).intValue());

        Assertions.assertEquals("Sucursal Sur", JsonPath.read(response, "$[1].sucursalName"));
        Assertions.assertEquals("Producto Uno", JsonPath.read(response, "$[1].productoName"));
        Assertions.assertEquals(30, ((Number) JsonPath.read(response, "$[1].stock")).intValue());
    }

    private UUID createFranquicia(String name, String idempotencyKey) {
        String response = webTestClient.post()
                .uri("/api/franquicias")
                .header("Content-Type", "application/json")
                .header("X-Client-Id", "cliente-producto-helper")
                .header("Idempotency-Key", idempotencyKey)
                .bodyValue("""
                                {
                                  "name": "%s"
                                }
                                """.formatted(name))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        return UUID.fromString(JsonPath.read(response, "$.id"));
    }

    private UUID createSucursal(String name, UUID franquiciaId, String idempotencyKey) {
        String response = webTestClient.post()
                .uri("/api/sucursales")
                .header("Content-Type", "application/json")
                .header("X-Client-Id", "cliente-producto-helper")
                .header("Idempotency-Key", idempotencyKey)
                .bodyValue("""
                                {
                                  "name": "%s",
                                  "franquiciaId": "%s"
                                }
                                """.formatted(name, franquiciaId))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        return UUID.fromString(JsonPath.read(response, "$.id"));
    }
}
