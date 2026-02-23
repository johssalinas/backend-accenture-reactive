package co.com.accenture.usecase.franquicia;

import co.com.accenture.model.exception.BusinessException;
import co.com.accenture.model.exception.message.BusinessErrorMessage;
import co.com.accenture.model.franquicia.Franquicia;
import co.com.accenture.model.franquicia.gateways.FranquiciaCacheRepository;
import co.com.accenture.model.franquicia.gateways.FranquiciaRepository;
import co.com.accenture.model.idempotency.IdempotencyRecord;
import co.com.accenture.model.idempotency.IdempotencyStatus;
import co.com.accenture.model.idempotency.gateways.IdempotencyRepository;
import co.com.accenture.usecase.franquicia.helper.FranquiciaIdempotencyCodec;
import co.com.accenture.usecase.idempotency.IdempotencyHashHelper;
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
class FranquiciaUseCaseTest {

        @Mock
        private FranquiciaRepository repository;

        @Mock
        private FranquiciaCacheRepository cacheRepository;

        @Mock
        private IdempotencyRepository idempotencyRepository;

        @InjectMocks
        private FranquiciaUseCase useCase;

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
        void saveShouldValidateAndAssignId() {
                Franquicia request = Franquicia.builder().name("  Franquicia Norte ").build();
                when(idempotencyRepository.tryAcquire(anyString(), anyString(), anyString()))
                                .thenReturn(Mono.just(true));
                when(repository.save(any(Franquicia.class)))
                                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
                when(idempotencyRepository.markCompleted(anyString(), anyString(), anyString()))
                                .thenReturn(Mono.empty());

                StepVerifier.create(useCase.save("cliente-1", "idem-1", request))
                                .assertNext(saved -> {
                                        assertNotNull(saved.getId());
                                        assertEquals("Franquicia Norte", saved.getName());
                                })
                                .verifyComplete();
        }

        @Test
        void saveShouldFailWhenNameIsBlank() {
                Franquicia request = Franquicia.builder().name("   ").build();
                when(idempotencyRepository.tryAcquire(anyString(), anyString(), anyString()))
                                .thenReturn(Mono.just(true));
                when(idempotencyRepository.release(anyString(), anyString())).thenReturn(Mono.empty());

                StepVerifier.create(useCase.save("cliente-1", "idem-1", request))
                                .expectErrorSatisfies(error -> {
                                        BusinessException businessException = (BusinessException) error;
                                        assertEquals(BusinessErrorMessage.INVALID_RESOURCE_NAME,
                                                        businessException.getBusinessErrorMessage());
                                })
                                .verify();
        }

        @Test
        void findByIdShouldFailWhenFranquiciaDoesNotExist() {
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
                Franquicia cached = Franquicia.builder().id(id).name("Cacheada").build();
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
                                Franquicia.builder().id(UUID.randomUUID()).name("A").build(),
                                Franquicia.builder().id(UUID.randomUUID()).name("B").build()));

                StepVerifier.create(useCase.findAll())
                                .expectNextCount(2)
                                .verifyComplete();
        }

        @Test
        void deleteByIdShouldDeleteWhenFranquiciaExists() {
                UUID id = UUID.randomUUID();
                when(repository.findById(id)).thenReturn(Mono.just(Franquicia.builder().id(id).name("A").build()));
                when(repository.deleteById(id)).thenReturn(Mono.empty());

                StepVerifier.create(useCase.deleteById(id))
                                .verifyComplete();

                verify(repository).deleteById(id);
        }

        @Test
        void updateNameShouldFailWhenRepositoryReturnsEmpty() {
                UUID id = UUID.randomUUID();
                when(repository.updateName(id, "Franquicia Sur")).thenReturn(Mono.empty());

                StepVerifier.create(useCase.updateName(id, "Franquicia Sur"))
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
                Franquicia updated = Franquicia.builder().id(id).name("Franquicia Sur").build();
                when(repository.updateName(id, "Franquicia Sur")).thenReturn(Mono.just(updated));

                StepVerifier.create(useCase.updateName(id, "Franquicia Sur"))
                                .assertNext(result -> assertEquals("Franquicia Sur", result.getName()))
                                .verifyComplete();

                verify(cacheRepository).putById(updated);
                verify(cacheRepository).evictAll();
        }

        @Test
        void saveShouldPersistAndMarkCompletedWhenLockIsAcquired() {
                Franquicia request = Franquicia.builder().name("Franquicia Centro").build();
                when(idempotencyRepository.tryAcquire(anyString(), anyString(), anyString()))
                                .thenReturn(Mono.just(true));
                when(repository.save(any(Franquicia.class)))
                                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
                when(idempotencyRepository.markCompleted(anyString(), anyString(), anyString()))
                                .thenReturn(Mono.empty());

                StepVerifier.create(useCase.save("cliente-1", "idem-1", request))
                                .assertNext(saved -> {
                                        assertNotNull(saved.getId());
                                        assertEquals("Franquicia Centro", saved.getName());
                                })
                                .verifyComplete();

                verify(idempotencyRepository).markCompleted(anyString(), anyString(), anyString());
        }

        @Test
        void saveShouldReplayStoredResponseWhenRequestWasAlreadyCompleted() {
                Franquicia request = Franquicia.builder().name("Franquicia Centro").build();
                Franquicia stored = Franquicia.builder().id(UUID.randomUUID()).name("Franquicia Centro").build();

                when(idempotencyRepository.tryAcquire(anyString(), anyString(), anyString()))
                                .thenReturn(Mono.just(false));
                when(idempotencyRepository.findByClientAndKey("cliente-1", "idem-1"))
                                .thenReturn(Mono.just(IdempotencyRecord.builder()
                                                .clientId("cliente-1")
                                                .idempotencyKey("idem-1")
                                                .requestHash(IdempotencyHashHelper
                                                                .hash(FranquiciaIdempotencyCodec.INSTANCE
                                                                                .serialize(Franquicia.builder().name(
                                                                                                "Franquicia Centro")
                                                                                                .build())))
                                                .status(IdempotencyStatus.COMPLETED)
                                                .responsePayload(FranquiciaIdempotencyCodec.INSTANCE.serialize(stored))
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
        void saveShouldFailWhenKeyIsReusedWithDifferentRequest() {
                Franquicia request = Franquicia.builder().name("Franquicia Norte").build();
                when(idempotencyRepository.tryAcquire(anyString(), anyString(), anyString()))
                                .thenReturn(Mono.just(false));
                when(idempotencyRepository.findByClientAndKey("cliente-1", "idem-1"))
                                .thenReturn(Mono.just(IdempotencyRecord.builder()
                                                .clientId("cliente-1")
                                                .idempotencyKey("idem-1")
                                                .requestHash(IdempotencyHashHelper
                                                                .hash(FranquiciaIdempotencyCodec.INSTANCE
                                                                                .serialize(Franquicia.builder()
                                                                                                .name("Otra franquicia")
                                                                                                .build())))
                                                .status(IdempotencyStatus.COMPLETED)
                                                .responsePayload(FranquiciaIdempotencyCodec.INSTANCE.serialize(
                                                                Franquicia.builder().id(UUID.randomUUID())
                                                                                .name("Otra franquicia").build()))
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
