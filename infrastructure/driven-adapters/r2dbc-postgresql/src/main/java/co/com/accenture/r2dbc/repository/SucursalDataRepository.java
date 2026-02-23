package co.com.accenture.r2dbc.repository;

import co.com.accenture.r2dbc.entities.SucursalEntity;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface SucursalDataRepository extends ReactiveCrudRepository<SucursalEntity, UUID>, ReactiveQueryByExampleExecutor<SucursalEntity> {
    @Modifying
    @Query("UPDATE sucursal SET name = :name, version = version + 1 WHERE id = CAST(:id AS UUID)")
    Mono<Integer> updateNameById(@Param("id") UUID id, @Param("name") String name);
}
