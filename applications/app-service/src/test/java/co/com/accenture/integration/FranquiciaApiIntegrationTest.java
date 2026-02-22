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
class FranquiciaApiIntegrationTest {

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
                String createdResponse = webTestClient.post()
                                .uri("/api/franquicias")
                                .header("Content-Type", "application/json")
                                .header("X-Client-Id", "cliente-crud")
                                .header("Idempotency-Key", "crud-franquicia-001")
                                .bodyValue("""
                                                {
                                                  "name": "Franquicia Real"
                                                }
                                                """)
                                .exchange()
                                .expectStatus().isCreated()
                                .expectBody(String.class)
                                .returnResult()
                                .getResponseBody();

                UUID id = UUID.fromString(JsonPath.read(createdResponse, "$.id"));

                webTestClient.get()
                                .uri("/api/franquicias/{id}", id)
                                .exchange()
                                .expectStatus().isOk()
                                .expectBody()
                                .jsonPath("$.id").isEqualTo(id.toString())
                                .jsonPath("$.name").isEqualTo("Franquicia Real");

                webTestClient.patch()
                                .uri("/api/franquicias/{id}", id)
                                .header("Content-Type", "application/json")
                                .bodyValue("""
                                                {
                                                  "name": "Franquicia Actualizada"
                                                }
                                                """)
                                .exchange()
                                .expectStatus().isOk()
                                .expectBody()
                                .jsonPath("$.name").isEqualTo("Franquicia Actualizada");

                webTestClient.delete()
                                .uri("/api/franquicias/{id}", id)
                                .exchange()
                                .expectStatus().isNoContent();

                webTestClient.get()
                                .uri("/api/franquicias/{id}", id)
                                .exchange()
                                .expectStatus().isNotFound()
                                .expectBody()
                                .jsonPath("$.code").isEqualTo("FRA404");
        }

        @Test
        void saveShouldReturnBadRequestWhenBodyIsInvalid() {
                webTestClient.post()
                                .uri("/api/franquicias")
                                .header("Content-Type", "application/json")
                                .header("X-Client-Id", "cliente-invalid")
                                .header("Idempotency-Key", "invalid-body-001")
                                .bodyValue("{}")
                                .exchange()
                                .expectStatus().isBadRequest()
                                .expectBody()
                                .jsonPath("$.code").isEqualTo("FRA4002");
        }

        @Test
        void saveShouldBeIdempotentForSameClientAndSameKey() {
                String firstResponse = webTestClient.post()
                                .uri("/api/franquicias")
                                .header("Content-Type", "application/json")
                                .header("X-Client-Id", "cliente-idempotente")
                                .header("Idempotency-Key", "save-franquicia-001")
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

                String secondResponse = webTestClient.post()
                                .uri("/api/franquicias")
                                .header("Content-Type", "application/json")
                                .header("X-Client-Id", "cliente-idempotente")
                                .header("Idempotency-Key", "save-franquicia-001")
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

                UUID firstId = UUID.fromString(JsonPath.read(firstResponse, "$.id"));
                UUID secondId = UUID.fromString(JsonPath.read(secondResponse, "$.id"));

                webTestClient.post()
                                .uri("/api/franquicias")
                                .header("Content-Type", "application/json")
                                .header("X-Client-Id", "cliente-idempotente")
                                .header("Idempotency-Key", "save-franquicia-001")
                                .bodyValue("""
                                                {
                                                  "name": "Franquicia Diferente"
                                                }
                                                """)
                                .exchange()
                                .expectStatus().is4xxClientError()
                                .expectBody()
                                .jsonPath("$.code").isEqualTo("FRA4091");

                org.junit.jupiter.api.Assertions.assertEquals(firstId, secondId);
        }
}
