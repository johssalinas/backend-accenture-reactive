package co.com.accenture.config;

import co.com.accenture.model.franquicia.gateways.FranquiciaRepository;
import co.com.accenture.model.franquicia.gateways.FranquiciaCacheRepository;
import co.com.accenture.model.idempotency.gateways.IdempotencyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.mockito.Mockito.mock;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UseCasesConfigTest {

    @Test
    void testUseCaseBeansExist() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class)) {
            assertTrue(context.containsBean("franquiciaUseCase"), "No se encontró el bean franquiciaUseCase");
        }
    }

    @Configuration
    @Import(UseCasesConfig.class)
    static class TestConfig {

        @Bean
        FranquiciaRepository franquiciaRepository() {
            return mock(FranquiciaRepository.class);
        }

        @Bean
        FranquiciaCacheRepository franquiciaCacheRepository() {
            return mock(FranquiciaCacheRepository.class);
        }

        @Bean
        IdempotencyRepository idempotencyRepository() {
            return mock(IdempotencyRepository.class);
        }
    }
}