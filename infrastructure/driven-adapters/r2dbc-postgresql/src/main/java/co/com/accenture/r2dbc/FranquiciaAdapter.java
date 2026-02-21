package co.com.accenture.r2dbc;

import co.com.accenture.model.franquicia.Franquicia;
import co.com.accenture.model.franquicia.gateways.FranquiciaRepository;
import co.com.accenture.r2dbc.entities.FranquiciaEntity;
import co.com.accenture.r2dbc.helper.ReactiveAdapterOperations;
import co.com.accenture.r2dbc.repository.FranquiciaDataRepository;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class FranquiciaAdapter extends ReactiveAdapterOperations<Franquicia, FranquiciaEntity, String, FranquiciaDataRepository> implements FranquiciaRepository {

    public FranquiciaAdapter(FranquiciaDataRepository repository, ObjectMapper mapper) {
        super(repository, mapper, d -> mapper.map(d, Franquicia.class));
    }

    @Override
    public Mono<Void> deleteById(String id) {
        return repository.deleteById(id);
    }

}
