package co.com.accenture.r2dbc.repository;

import co.com.accenture.r2dbc.entities.FranquiciaEntity;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface FranquiciaDataRepository extends ReactiveCrudRepository<FranquiciaEntity, String>, ReactiveQueryByExampleExecutor<FranquiciaEntity> {
}
