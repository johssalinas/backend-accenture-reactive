package co.com.accenture.api.franquicia;

import co.com.accenture.api.config.CorsConfig;
import co.com.accenture.api.config.SecurityHeadersConfig;
import co.com.accenture.model.exception.BusinessException;
import co.com.accenture.model.exception.message.BusinessErrorMessage;
import co.com.accenture.model.franquicia.Franquicia;
import co.com.accenture.usecase.franquicia.FranquiciaUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = { FranquiciaRouterRest.class, FranquiciaHandler.class })
@WebFluxTest
@Import({ CorsConfig.class, SecurityHeadersConfig.class })
class FranquiciaRouterRestTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private FranquiciaUseCase useCase;

    @Test
    void saveShouldReturnCreated() {
        UUID id = UUID.randomUUID();
        Franquicia response = Franquicia.builder().id(id).name("Franquicia Centro").build();

        when(useCase.save(any(Franquicia.class))).thenReturn(Mono.just(response));

        webTestClient.post()
                .uri("/api/franquicias")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "name": "Franquicia Centro"
                        }
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().valueEquals("Location", "/api/franquicias/" + id)
                .expectBody()
                .jsonPath("$.name").isEqualTo("Franquicia Centro");
    }

    @Test
    void findByIdShouldReturnBadRequestWhenIdIsInvalid() {
        webTestClient.get()
                .uri("/api/franquicias/id-invalido")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("FRA4001");
    }

    @Test
    void updateNameShouldReturnNotFound() {
        UUID id = UUID.randomUUID();
        when(useCase.updateName(eq(id), eq("Nueva")))
                .thenReturn(Mono.error(new BusinessException(BusinessErrorMessage.FRANQUICIA_NOT_FOUND)));

        webTestClient.patch()
                .uri("/api/franquicias/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "name": "Nueva"
                        }
                        """)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("FRA404");
    }

    @Test
    void deleteShouldReturnNoContent() {
        UUID id = UUID.randomUUID();
        when(useCase.deleteById(id)).thenReturn(Mono.empty());

        webTestClient.delete()
                .uri("/api/franquicias/{id}", id)
                .exchange()
                                .expectStatus().isNoContent()
                                .expectHeader().valueEquals("X-Content-Type-Options", "nosniff");
    }
}
