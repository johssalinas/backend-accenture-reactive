package co.com.accenture.r2dbc;

import co.com.accenture.model.franquicia.Franquicia;
import co.com.accenture.model.franquicia.gateways.FranquiciaRepository;
import co.com.accenture.r2dbc.entities.FranquiciaEntity;
import co.com.accenture.r2dbc.helper.ReactiveAdapterOperations;
import co.com.accenture.r2dbc.repository.FranquiciaDataRepository;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public class FranquiciaAdapter extends ReactiveAdapterOperations<Franquicia, FranquiciaEntity, UUID, FranquiciaDataRepository> implements FranquiciaRepository {

    public FranquiciaAdapter(FranquiciaDataRepository repository, ObjectMapper mapper) {
        super(repository, mapper, data -> Franquicia.builder()
                .id(data.getId())
                .name(data.getName())
                .build());
    }

    @Override
    public Mono<Void> deleteById(UUID id) {
        return repository.deleteById(id);
    }

    @Override
    public Mono<Franquicia> updateName(UUID id, String name) {
        return repository.updateNameById(id, name)
                .filter(rowsUpdated -> rowsUpdated > 0)
                .flatMap(rowsUpdated -> repository.findById(id))
                .map(this::toEntity);
    }

}
