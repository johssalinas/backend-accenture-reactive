package co.com.accenture.r2dbc;

import co.com.accenture.model.franquicia.Franquicia;
import co.com.accenture.r2dbc.entities.FranquiciaEntity;
import co.com.accenture.r2dbc.repository.FranquiciaDataRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivecommons.utils.ObjectMapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MyReactiveRepositoryAdapterTest {
    @InjectMocks
    FranquiciaAdapter repositoryAdapter;

    @Mock
    FranquiciaDataRepository repository;

    @Mock
    ObjectMapper mapper;

    @Test
    void mustFindValueById() {
        UUID id = UUID.randomUUID();
        FranquiciaEntity entity = FranquiciaEntity.builder().id(id).name("Franquicia Norte").build();

        when(repository.findById(id)).thenReturn(Mono.just(entity));

        Mono<Franquicia> result = repositoryAdapter.findById(id);

        StepVerifier.create(result)
                .expectNextMatches(value -> value.getId().equals(id) && value.getName().equals("Franquicia Norte"))
                .verifyComplete();
    }

    @Test
    void mustFindAllValues() {
        UUID id = UUID.randomUUID();
        when(repository.findAll()).thenReturn(Flux.just(FranquiciaEntity.builder().id(id).name("Franquicia Centro").build()));

        Flux<Franquicia> result = repositoryAdapter.findAll();

        StepVerifier.create(result)
                .expectNextMatches(value -> value.getName().equals("Franquicia Centro"))
                .verifyComplete();
    }

    @Test
    void mustSaveValue() {
        Franquicia request = Franquicia.builder().id(UUID.randomUUID()).name("Franquicia Sur").build();
        FranquiciaEntity mapped = FranquiciaEntity.builder().id(request.getId()).name(request.getName()).build();

        when(mapper.map(request, FranquiciaEntity.class)).thenReturn(mapped);
        when(repository.save(mapped)).thenReturn(Mono.just(mapped));

        Mono<Franquicia> result = repositoryAdapter.save(request);

        StepVerifier.create(result)
                .expectNextMatches(value -> value.getId().equals(request.getId()) && value.getName().equals("Franquicia Sur"))
                .verifyComplete();
    }

    @Test
    void updateNameShouldReturnEmptyWhenNoRowsAreUpdated() {
        UUID id = UUID.randomUUID();
        when(repository.updateNameById(id, "Nueva")).thenReturn(Mono.just(0));

        StepVerifier.create(repositoryAdapter.updateName(id, "Nueva"))
                .verifyComplete();

        verify(repository).updateNameById(id, "Nueva");
    }

    @Test
    void deleteByIdShouldComplete() {
        UUID id = UUID.randomUUID();
        when(repository.deleteById(id)).thenReturn(Mono.empty());

        StepVerifier.create(repositoryAdapter.deleteById(id))
                .verifyComplete();
    }
}
