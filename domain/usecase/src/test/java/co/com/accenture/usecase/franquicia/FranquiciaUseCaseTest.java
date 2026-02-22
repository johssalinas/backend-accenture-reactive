package co.com.accenture.usecase.franquicia;

import co.com.accenture.model.exception.BusinessException;
import co.com.accenture.model.exception.message.BusinessErrorMessage;
import co.com.accenture.model.franquicia.Franquicia;
import co.com.accenture.model.franquicia.gateways.FranquiciaRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FranquiciaUseCaseTest {

    @Mock
    private FranquiciaRepository repository;

    @InjectMocks
    private FranquiciaUseCase useCase;

    @Test
    void saveShouldValidateAndAssignId() {
        Franquicia request = Franquicia.builder().name("  Franquicia Norte ").build();
        when(repository.save(any(Franquicia.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(useCase.save(request))
                .assertNext(saved -> {
                    assertNotNull(saved.getId());
                    assertEquals("Franquicia Norte", saved.getName());
                })
                .verifyComplete();
    }

    @Test
    void saveShouldFailWhenNameIsBlank() {
        Franquicia request = Franquicia.builder().name("   ").build();

        StepVerifier.create(useCase.save(request))
                .expectErrorSatisfies(error -> {
                    BusinessException businessException = (BusinessException) error;
                    assertEquals(BusinessErrorMessage.INVALID_FRANQUICIA_NAME, businessException.getBusinessErrorMessage());
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
                    assertEquals(BusinessErrorMessage.FRANQUICIA_NOT_FOUND, businessException.getBusinessErrorMessage());
                })
                .verify();
    }

    @Test
    void findAllShouldReturnRepositoryValues() {
        when(repository.findAll()).thenReturn(Flux.just(
                Franquicia.builder().id(UUID.randomUUID()).name("A").build(),
                Franquicia.builder().id(UUID.randomUUID()).name("B").build()
        ));

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
                    assertEquals(BusinessErrorMessage.FRANQUICIA_NOT_FOUND, businessException.getBusinessErrorMessage());
                })
                .verify();
    }
}
