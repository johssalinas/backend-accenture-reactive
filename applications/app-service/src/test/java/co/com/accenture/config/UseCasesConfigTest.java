package co.com.accenture.config;

import co.com.accenture.model.franquicia.gateways.FranquiciaRepository;
import co.com.accenture.model.franquicia.gateways.FranquiciaCacheRepository;
import co.com.accenture.model.idempotency.gateways.IdempotencyRepository;
import co.com.accenture.model.producto.gateways.ProductoCacheRepository;
import co.com.accenture.model.producto.gateways.ProductoRepository;
import co.com.accenture.model.sucursal.gateways.SucursalCacheRepository;
import co.com.accenture.model.sucursal.gateways.SucursalRepository;
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
            assertTrue(context.containsBean("sucursalUseCase"), "No se encontró el bean sucursalUseCase");
            assertTrue(context.containsBean("productoUseCase"), "No se encontró el bean productoUseCase");
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

        @Bean
        SucursalRepository sucursalRepository() {
            return mock(SucursalRepository.class);
        }

        @Bean
        SucursalCacheRepository sucursalCacheRepository() {
            return mock(SucursalCacheRepository.class);
        }

        @Bean
        ProductoRepository productoRepository() {
            return mock(ProductoRepository.class);
        }

        @Bean
        ProductoCacheRepository productoCacheRepository() {
            return mock(ProductoCacheRepository.class);
        }
    }
}