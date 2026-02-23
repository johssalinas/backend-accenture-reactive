package co.com.accenture.integration;

import co.com.accenture.MainApplication;
import com.jayway.jsonpath.JsonPath;
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
class SucursalApiIntegrationTest {

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
        String franquiciaResponse = webTestClient.post()
                .uri("/api/franquicias")
                .header("Content-Type", "application/json")
                .header("X-Client-Id", "cliente-sucursal-crud")
                .header("Idempotency-Key", "crud-franquicia-sucursal-001")
                .bodyValue("""
                                {
                                  "name": "Franquicia Matriz"
                                }
                                """)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        UUID franquiciaId = UUID.fromString(JsonPath.read(franquiciaResponse, "$.id"));

        String createdResponse = webTestClient.post()
                .uri("/api/sucursales")
                .header("Content-Type", "application/json")
                .header("X-Client-Id", "cliente-sucursal-crud")
                .header("Idempotency-Key", "crud-sucursal-001")
                .bodyValue("""
                                {
                                  "name": "Sucursal Real",
                                  "franquiciaId": "%s"
                                }
                                """.formatted(franquiciaId))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        UUID id = UUID.fromString(JsonPath.read(createdResponse, "$.id"));

        webTestClient.get()
                .uri("/api/sucursales/{id}", id)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(id.toString())
                .jsonPath("$.name").isEqualTo("Sucursal Real")
                .jsonPath("$.franquiciaId").isEqualTo(franquiciaId.toString());

        webTestClient.patch()
                .uri("/api/sucursales/{id}", id)
                .header("Content-Type", "application/json")
                .bodyValue("""
                                {
                                  "name": "Sucursal Actualizada"
                                }
                                """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.name").isEqualTo("Sucursal Actualizada");

        webTestClient.delete()
                .uri("/api/sucursales/{id}", id)
                .exchange()
                .expectStatus().isNoContent();

        webTestClient.get()
                .uri("/api/sucursales/{id}", id)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("BUS4040");
    }

    @Test
    void saveShouldFailWhenFranquiciaIdDoesNotExist() {
        webTestClient.post()
                .uri("/api/sucursales")
                .header("Content-Type", "application/json")
                .header("X-Client-Id", "cliente-sucursal-invalid")
                .header("Idempotency-Key", "save-sucursal-invalid-franquicia-001")
                .bodyValue("""
                                {
                                  "name": "Sucursal Huérfana",
                                  "franquiciaId": "%s"
                                }
                                """.formatted(UUID.randomUUID()))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("BUS4040");
    }

    @Test
    void saveShouldBeIdempotentForSameClientAndSameKey() {
        String franquiciaResponse = webTestClient.post()
                .uri("/api/franquicias")
                .header("Content-Type", "application/json")
                .header("X-Client-Id", "cliente-sucursal-idempotente")
                .header("Idempotency-Key", "create-franquicia-for-sucursal-001")
                .bodyValue("""
                                {
                                  "name": "Franquicia Idempotente"
                                }
                                """)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        UUID franquiciaId = UUID.fromString(JsonPath.read(franquiciaResponse, "$.id"));

        String firstResponse = webTestClient.post()
                .uri("/api/sucursales")
                .header("Content-Type", "application/json")
                .header("X-Client-Id", "cliente-sucursal-idempotente")
                .header("Idempotency-Key", "save-sucursal-001")
                .bodyValue("""
                                {
                                  "name": "Sucursal Idempotente",
                                  "franquiciaId": "%s"
                                }
                                """.formatted(franquiciaId))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        String secondResponse = webTestClient.post()
                .uri("/api/sucursales")
                .header("Content-Type", "application/json")
                .header("X-Client-Id", "cliente-sucursal-idempotente")
                .header("Idempotency-Key", "save-sucursal-001")
                .bodyValue("""
                                {
                                  "name": "Sucursal Idempotente",
                                  "franquiciaId": "%s"
                                }
                                """.formatted(franquiciaId))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        UUID firstId = UUID.fromString(JsonPath.read(firstResponse, "$.id"));
        UUID secondId = UUID.fromString(JsonPath.read(secondResponse, "$.id"));

        webTestClient.post()
                .uri("/api/sucursales")
                .header("Content-Type", "application/json")
                .header("X-Client-Id", "cliente-sucursal-idempotente")
                .header("Idempotency-Key", "save-sucursal-001")
                .bodyValue("""
                                {
                                  "name": "Sucursal Diferente",
                                  "franquiciaId": "%s"
                                }
                                """.formatted(franquiciaId))
                .exchange()
                .expectStatus().is4xxClientError()
                .expectBody()
                .jsonPath("$.code").isEqualTo("BUS4091");

        org.junit.jupiter.api.Assertions.assertEquals(firstId, secondId);
    }
}
