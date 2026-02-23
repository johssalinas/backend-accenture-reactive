package co.com.accenture.usecase.sucursal;

import co.com.accenture.model.exception.BusinessException;
import co.com.accenture.model.exception.message.BusinessErrorMessage;
import co.com.accenture.model.franquicia.Franquicia;
import co.com.accenture.model.franquicia.gateways.FranquiciaRepository;
import co.com.accenture.model.idempotency.IdempotencyRecord;
import co.com.accenture.model.idempotency.IdempotencyStatus;
import co.com.accenture.model.idempotency.gateways.IdempotencyRepository;
import co.com.accenture.model.sucursal.Sucursal;
import co.com.accenture.model.sucursal.gateways.SucursalCacheRepository;
import co.com.accenture.model.sucursal.gateways.SucursalRepository;
import co.com.accenture.usecase.idempotency.IdempotencyHashHelper;
import co.com.accenture.usecase.sucursal.helper.SucursalIdempotencyCodec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

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
class SucursalUseCaseTest {

    @Mock
    private SucursalRepository repository;

    @Mock
    private SucursalCacheRepository cacheRepository;

    @Mock
    private FranquiciaRepository franquiciaRepository;

    @Mock
    private IdempotencyRepository idempotencyRepository;

    @InjectMocks
    private SucursalUseCase useCase;

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
        UUID franquiciaId = UUID.randomUUID();
        Sucursal request = Sucursal.builder().name("  Sucursal Norte ").franquiciaId(franquiciaId).build();

        when(idempotencyRepository.tryAcquire(anyString(), anyString(), anyString()))
                .thenReturn(Mono.just(true));
        when(franquiciaRepository.findById(franquiciaId))
                .thenReturn(Mono.just(Franquicia.builder().id(franquiciaId).name("Franquicia A").build()));
        when(repository.save(any(Sucursal.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(idempotencyRepository.markCompleted(anyString(), anyString(), anyString()))
                .thenReturn(Mono.empty());

        StepVerifier.create(useCase.save("cliente-1", "idem-1", request))
                .assertNext(saved -> {
                    assertNotNull(saved.getId());
                    assertEquals("Sucursal Norte", saved.getName());
                    assertEquals(franquiciaId, saved.getFranquiciaId());
                })
                .verifyComplete();
    }

    @Test
    void saveShouldFailWhenFranquiciaDoesNotExist() {
        UUID franquiciaId = UUID.randomUUID();
        Sucursal request = Sucursal.builder().name("Sucursal Norte").franquiciaId(franquiciaId).build();

        when(idempotencyRepository.tryAcquire(anyString(), anyString(), anyString()))
                .thenReturn(Mono.just(true));
        when(franquiciaRepository.findById(franquiciaId)).thenReturn(Mono.empty());
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
    void findByIdShouldFailWhenSucursalDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.findById(id))
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
        Sucursal cached = Sucursal.builder().id(id).name("Cacheada").franquiciaId(UUID.randomUUID()).build();
        when(cacheRepository.getById(id)).thenReturn(Mono.just(cached));

        StepVerifier.create(useCase.findById(id))
                .assertNext(found -> {
                    assertEquals(id, found.getId());
                    assertEquals("Cacheada", found.getName());
                })
                .verifyComplete();

        verify(repository, never()).findById(id);
    }

    @Test
    void findAllShouldReturnRepositoryValues() {
        when(repository.findAll()).thenReturn(Flux.just(
                Sucursal.builder().id(UUID.randomUUID()).name("A").franquiciaId(UUID.randomUUID()).build(),
                Sucursal.builder().id(UUID.randomUUID()).name("B").franquiciaId(UUID.randomUUID()).build()));

        StepVerifier.create(useCase.findAll())
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void deleteByIdShouldDeleteWhenSucursalExists() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id))
                .thenReturn(Mono.just(Sucursal.builder().id(id).name("A").franquiciaId(UUID.randomUUID()).build()));
        when(repository.deleteById(id)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.deleteById(id))
                .verifyComplete();

        verify(repository).deleteById(id);
    }

    @Test
    void updateNameShouldFailWhenRepositoryReturnsEmpty() {
        UUID id = UUID.randomUUID();
        when(repository.updateName(id, "Sucursal Sur")).thenReturn(Mono.empty());

        StepVerifier.create(useCase.updateName(id, "Sucursal Sur"))
                .expectErrorSatisfies(error -> {
                    BusinessException businessException = (BusinessException) error;
                    assertEquals(BusinessErrorMessage.RESOURCE_NOT_FOUND,
                            businessException.getBusinessErrorMessage());
                })
                .verify();
    }

    @Test
    void updateNameShouldRefreshByIdAndInvalidateAllCache() {
        UUID id = UUID.randomUUID();
        Sucursal updated = Sucursal.builder().id(id).name("Sucursal Sur").franquiciaId(UUID.randomUUID()).build();
        when(repository.updateName(id, "Sucursal Sur")).thenReturn(Mono.just(updated));

        StepVerifier.create(useCase.updateName(id, "Sucursal Sur"))
                .assertNext(result -> assertEquals("Sucursal Sur", result.getName()))
                .verifyComplete();

        verify(cacheRepository).putById(updated);
        verify(cacheRepository).evictAll();
    }

    @Test
    void saveShouldReplayStoredResponseWhenRequestWasAlreadyCompleted() {
        UUID franquiciaId = UUID.randomUUID();
        Sucursal request = Sucursal.builder().name("Sucursal Centro").franquiciaId(franquiciaId).build();
        Sucursal stored = Sucursal.builder().id(UUID.randomUUID()).name("Sucursal Centro").franquiciaId(franquiciaId).build();

        when(idempotencyRepository.tryAcquire(anyString(), anyString(), anyString()))
                .thenReturn(Mono.just(false));
        when(idempotencyRepository.findByClientAndKey("cliente-1", "idem-1"))
                .thenReturn(Mono.just(IdempotencyRecord.builder()
                        .clientId("cliente-1")
                        .idempotencyKey("idem-1")
                        .requestHash(IdempotencyHashHelper.hash(SucursalIdempotencyCodec.INSTANCE.serialize(request)))
                        .status(IdempotencyStatus.COMPLETED)
                        .responsePayload(SucursalIdempotencyCodec.INSTANCE.serialize(stored))
                        .build()));

        StepVerifier.create(useCase.save("cliente-1", "idem-1", request))
                .assertNext(replayed -> {
                    assertEquals(stored.getId(), replayed.getId());
                    assertEquals(stored.getName(), replayed.getName());
                    assertEquals(stored.getFranquiciaId(), replayed.getFranquiciaId());
                })
                .verifyComplete();

        verifyNoInteractions(repository);
    }

    @Test
    void saveShouldFailWhenKeyIsReusedWithDifferentRequest() {
        Sucursal request = Sucursal.builder().name("Sucursal Norte").franquiciaId(UUID.randomUUID()).build();

        when(idempotencyRepository.tryAcquire(anyString(), anyString(), anyString()))
                .thenReturn(Mono.just(false));
        when(idempotencyRepository.findByClientAndKey("cliente-1", "idem-1"))
                .thenReturn(Mono.just(IdempotencyRecord.builder()
                        .clientId("cliente-1")
                        .idempotencyKey("idem-1")
                        .requestHash(IdempotencyHashHelper
                                .hash(SucursalIdempotencyCodec.INSTANCE.serialize(
                                        Sucursal.builder().name("Otra sucursal").franquiciaId(UUID.randomUUID()).build())))
                        .status(IdempotencyStatus.COMPLETED)
                        .responsePayload(SucursalIdempotencyCodec.INSTANCE.serialize(
                                Sucursal.builder().id(UUID.randomUUID()).name("Otra sucursal")
                                        .franquiciaId(UUID.randomUUID()).build()))
                        .build()));

        StepVerifier.create(useCase.save("cliente-1", "idem-1", request))
                .expectErrorSatisfies(error -> {
                    BusinessException businessException = (BusinessException) error;
                    assertEquals(BusinessErrorMessage.IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_REQUEST,
                            businessException.getBusinessErrorMessage());
                })
                .verify();
    }
}
