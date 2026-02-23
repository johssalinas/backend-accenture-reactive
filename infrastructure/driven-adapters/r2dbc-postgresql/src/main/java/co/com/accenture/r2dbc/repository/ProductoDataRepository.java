package co.com.accenture.r2dbc.repository;

import co.com.accenture.r2dbc.entities.ProductoEntity;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ProductoDataRepository extends ReactiveCrudRepository<ProductoEntity, UUID>,
        ReactiveQueryByExampleExecutor<ProductoEntity> {

    @Modifying
    @Query("UPDATE producto SET name = :name WHERE id = CAST(:id AS UUID)")
    Mono<Integer> updateNameById(@Param("id") UUID id, @Param("name") String name);
}
