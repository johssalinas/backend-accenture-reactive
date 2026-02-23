package co.com.accenture.usecase.producto;

import co.com.accenture.model.exception.BusinessException;
import co.com.accenture.model.exception.message.BusinessErrorMessage;
import co.com.accenture.model.franquicia.Franquicia;
import co.com.accenture.model.franquicia.gateways.FranquiciaRepository;
import co.com.accenture.model.idempotency.IdempotencyRecord;
import co.com.accenture.model.idempotency.IdempotencyStatus;
import co.com.accenture.model.idempotency.gateways.IdempotencyRepository;
import co.com.accenture.model.producto.Producto;
import co.com.accenture.model.producto.ProductoMaxStockPorSucursal;
import co.com.accenture.model.producto.ProductoStock;
import co.com.accenture.model.producto.gateways.ProductoCacheRepository;
import co.com.accenture.model.producto.gateways.ProductoRepository;
import co.com.accenture.model.sucursal.Sucursal;
import co.com.accenture.model.sucursal.gateways.SucursalRepository;
import co.com.accenture.usecase.idempotency.IdempotencyHashHelper;
import co.com.accenture.usecase.producto.helper.ProductoIdempotencyCodec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductoUseCaseTest {

    @Mock
    private ProductoRepository repository;

    @Mock
    private ProductoCacheRepository cacheRepository;

    @Mock
    private SucursalRepository sucursalRepository;

    @Mock
    private FranquiciaRepository franquiciaRepository;

    @Mock
    private IdempotencyRepository idempotencyRepository;

    @InjectMocks
    private ProductoUseCase useCase;

    @BeforeEach
    void setUp() {
        lenient().when(cacheRepository.getById(any())).thenReturn(Mono.empty());
        lenient().when(cacheRepository.getAll()).thenReturn(Flux.empty());
        lenient().when(cacheRepository.putById(any())).thenReturn(Mono.empty());
        lenient().when(cacheRepository.putAll(any())).thenReturn(Mono.empty());
        lenient().when(cacheRepository.evictById(any())).thenReturn(Mono.empty());
        lenient().when(cacheRepository.evictAll()).thenReturn(Mono.empty());
    }

    @Test
    void saveShouldValidateAssignIdAndPersist() {
        UUID sucursalId = UUID.randomUUID();
        Producto request = Producto.builder()
                .name("  Producto A ")
                .sucursales(List.of(ProductoStock.builder().sucursalId(sucursalId).stock(15).build()))
                .build();

        when(idempotencyRepository.tryAcquire(anyString(), anyString(), anyString()))
                .thenReturn(Mono.just(true));
        when(sucursalRepository.findById(sucursalId))
                .thenReturn(Mono.just(Sucursal.builder().id(sucursalId).name("Sucursal A").build()));
        when(repository.save(any(Producto.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(idempotencyRepository.markCompleted(anyString(), anyString(), anyString()))
                .thenReturn(Mono.empty());

        StepVerifier.create(useCase.save("cliente-1", "idem-1", request))
                .assertNext(saved -> {
                    assertNotNull(saved.getId());
                    assertEquals("Producto A", saved.getName());
                    assertEquals(1, saved.getSucursales().size());
                    assertEquals(15, saved.getSucursales().get(0).getStock());
                })
                .verifyComplete();
    }

    @Test
    void saveShouldFailWhenStockIsNegative() {
        UUID sucursalId = UUID.randomUUID();
        Producto request = Producto.builder()
                .name("Producto A")
                .sucursales(List.of(ProductoStock.builder().sucursalId(sucursalId).stock(-1).build()))
                .build();

        when(idempotencyRepository.tryAcquire(anyString(), anyString(), anyString()))
                .thenReturn(Mono.just(true));
        when(idempotencyRepository.release(anyString(), anyString())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.save("cliente-1", "idem-1", request))
                .expectErrorSatisfies(error -> {
                    BusinessException businessException = (BusinessException) error;
                    assertEquals(BusinessErrorMessage.INVALID_RESOURCE_STOCK,
                            businessException.getBusinessErrorMessage());
                })
                .verify();
    }

    @Test
    void saveShouldFailWhenSucursalDoesNotExist() {
        UUID sucursalId = UUID.randomUUID();
        Producto request = Producto.builder()
                .name("Producto A")
                .sucursales(List.of(ProductoStock.builder().sucursalId(sucursalId).stock(2).build()))
                .build();

        when(idempotencyRepository.tryAcquire(anyString(), anyString(), anyString()))
                .thenReturn(Mono.just(true));
        when(sucursalRepository.findById(sucursalId)).thenReturn(Mono.empty());
        when(idempotencyRepository.release(anyString(), anyString())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.save("cliente-1", "idem-1", request))
                .expectErrorSatisfies(error -> {
                    BusinessException businessException = (BusinessException) error;
                    assertEquals(BusinessErrorMessage.RESOURCE_NOT_FOUND,
                            businessException.getBusinessErrorMessage());
                })
                .verify();
    }

    @Test
    void findByIdShouldReturnCacheValueWithoutCallingRepository() {
        UUID id = UUID.randomUUID();
        Producto cached = Producto.builder().id(id).name("Cacheado").sucursales(List.of()).build();
        when(cacheRepository.getById(id)).thenReturn(Mono.just(cached));

        StepVerifier.create(useCase.findById(id))
                .assertNext(found -> {
                    assertEquals(id, found.getId());
                    assertEquals("Cacheado", found.getName());
                })
                .verifyComplete();

        verify(repository, never()).findById(id);
    }

    @Test
    void updateNameShouldRefreshByIdAndInvalidateAllCache() {
        UUID id = UUID.randomUUID();
        Producto updated = Producto.builder().id(id).name("Producto Actualizado").sucursales(List.of()).build();
        when(repository.updateName(id, "Producto Actualizado")).thenReturn(Mono.just(updated));

        StepVerifier.create(useCase.updateName(id, "Producto Actualizado"))
                .assertNext(result -> assertEquals("Producto Actualizado", result.getName()))
                .verifyComplete();

        verify(cacheRepository).putById(updated);
        verify(cacheRepository).evictAll();
    }

    @Test
    void saveShouldReplayStoredResponseWhenRequestWasAlreadyCompleted() {
        UUID sucursalId = UUID.randomUUID();
        Producto request = Producto.builder()
                .name("Producto A")
                .sucursales(List.of(ProductoStock.builder().sucursalId(sucursalId).stock(5).build()))
                .build();
        Producto stored = Producto.builder()
                .id(UUID.randomUUID())
                .name("Producto A")
                .sucursales(List.of(ProductoStock.builder().sucursalId(sucursalId).stock(5).build()))
                .build();

        when(idempotencyRepository.tryAcquire(anyString(), anyString(), anyString()))
                .thenReturn(Mono.just(false));
        when(idempotencyRepository.findByClientAndKey("cliente-1", "idem-1"))
                .thenReturn(Mono.just(IdempotencyRecord.builder()
                        .clientId("cliente-1")
                        .idempotencyKey("idem-1")
                        .requestHash(IdempotencyHashHelper.hash(ProductoIdempotencyCodec.INSTANCE.serialize(request)))
                        .status(IdempotencyStatus.COMPLETED)
                        .responsePayload(ProductoIdempotencyCodec.INSTANCE.serialize(stored))
                        .build()));

        StepVerifier.create(useCase.save("cliente-1", "idem-1", request))
                .assertNext(replayed -> {
                    assertEquals(stored.getId(), replayed.getId());
                    assertEquals(stored.getName(), replayed.getName());
                })
                .verifyComplete();

        verifyNoInteractions(repository);
    }

    @Test
    void findMaxStockBySucursalForFranquiciaShouldReturnValues() {
        UUID franquiciaId = UUID.randomUUID();
        ProductoMaxStockPorSucursal result = ProductoMaxStockPorSucursal.builder()
                .sucursalId(UUID.randomUUID())
                .sucursalName("Sucursal Centro")
                .productoId(UUID.randomUUID())
                .productoName("Producto Mayor Stock")
                .stock(99)
                .build();

        when(franquiciaRepository.findById(franquiciaId))
                .thenReturn(Mono.just(Franquicia.builder().id(franquiciaId).name("Franquicia A").build()));
        when(repository.findMaxStockBySucursalForFranquicia(franquiciaId)).thenReturn(Flux.just(result));

        StepVerifier.create(useCase.findMaxStockBySucursalForFranquicia(franquiciaId))
                .assertNext(item -> {
                    assertEquals("Sucursal Centro", item.getSucursalName());
                    assertEquals("Producto Mayor Stock", item.getProductoName());
                    assertEquals(99, item.getStock());
                })
                .verifyComplete();
    }
}
