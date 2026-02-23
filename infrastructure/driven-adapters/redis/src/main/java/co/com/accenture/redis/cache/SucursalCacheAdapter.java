package co.com.accenture.redis.cache;

import co.com.accenture.model.sucursal.Sucursal;
import co.com.accenture.model.sucursal.gateways.SucursalCacheRepository;
import co.com.accenture.redis.config.RedisConnectionProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Repository
public class SucursalCacheAdapter extends GenericRedisCacheAdapter<Sucursal> implements SucursalCacheRepository {

    private static final TypeReference<List<Sucursal>> SUCURSAL_LIST_TYPE = new TypeReference<>() {
    };

    public SucursalCacheAdapter(ReactiveStringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            RedisConnectionProperties properties) {
        super(redisTemplate, objectMapper, properties, "sucursal", Sucursal.class, SUCURSAL_LIST_TYPE);
    }

    @Override
    public Mono<Sucursal> getById(UUID id) {
        return super.getByIdResource(id);
    }

    @Override
    public Mono<Void> putById(Sucursal sucursal) {
        return sucursal == null ? Mono.empty() : super.putByIdResource(sucursal.getId(), sucursal);
    }

    @Override
    public Flux<Sucursal> getAll() {
        return super.getAllResources();
    }

    @Override
    public Mono<Void> putAll(List<Sucursal> sucursales) {
        return super.putAllResources(sucursales);
    }

    @Override
    public Mono<Void> evictById(UUID id) {
        return super.evictByIdResource(id);
    }

    @Override
    public Mono<Void> evictAll() {
        return super.evictAllResources();
    }
}
