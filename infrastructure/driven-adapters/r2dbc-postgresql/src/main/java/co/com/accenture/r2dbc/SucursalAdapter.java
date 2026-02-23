package co.com.accenture.r2dbc;

import co.com.accenture.model.sucursal.Sucursal;
import co.com.accenture.model.sucursal.gateways.SucursalRepository;
import co.com.accenture.r2dbc.entities.SucursalEntity;
import co.com.accenture.r2dbc.helper.DatabaseExceptionMapper;
import co.com.accenture.r2dbc.helper.ReactiveAdapterOperations;
import co.com.accenture.r2dbc.repository.SucursalDataRepository;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public class SucursalAdapter extends ReactiveAdapterOperations<Sucursal, SucursalEntity, UUID, SucursalDataRepository>
        implements SucursalRepository {

    public SucursalAdapter(SucursalDataRepository repository, ObjectMapper mapper) {
        super(repository, mapper, data -> Sucursal.builder()
                .id(data.getId())
                .name(data.getName())
                .franquiciaId(data.getFranquiciaId())
                .build());
    }

    @Override
    public Mono<Sucursal> save(Sucursal entity) {
        return super.save(entity)
                .onErrorMap(DatabaseExceptionMapper::map);
    }

    @Override
    public Mono<Sucursal> findById(UUID id) {
        return super.findById(id)
                .onErrorMap(DatabaseExceptionMapper::map);
    }

    @Override
    public Flux<Sucursal> findAll() {
        return super.findAll()
                .onErrorMap(DatabaseExceptionMapper::map);
    }

    @Override
    public Mono<Void> deleteById(UUID id) {
        return repository.deleteById(id)
                .onErrorMap(DatabaseExceptionMapper::map);
    }

    @Override
    public Mono<Sucursal> updateName(UUID id, String name) {
        return repository.updateNameById(id, name)
                .filter(rowsUpdated -> rowsUpdated > 0)
                .flatMap(rowsUpdated -> repository.findById(id))
                .map(this::toEntity)
                .onErrorMap(DatabaseExceptionMapper::map);
    }
}
