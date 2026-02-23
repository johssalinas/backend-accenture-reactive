package co.com.accenture.redis.cache;

import co.com.accenture.model.producto.Producto;
import co.com.accenture.model.producto.gateways.ProductoCacheRepository;
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
public class ProductoCacheAdapter extends GenericRedisCacheAdapter<Producto> implements ProductoCacheRepository {

    private static final TypeReference<List<Producto>> PRODUCTO_LIST_TYPE = new TypeReference<>() {
    };

    public ProductoCacheAdapter(ReactiveStringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            RedisConnectionProperties properties) {
        super(redisTemplate, objectMapper, properties, "producto", Producto.class, PRODUCTO_LIST_TYPE);
    }

    @Override
    public Mono<Producto> getById(UUID id) {
        return super.getByIdResource(id);
    }

    @Override
    public Mono<Void> putById(Producto producto) {
        return producto == null ? Mono.empty() : super.putByIdResource(producto.getId(), producto);
    }

    @Override
    public Flux<Producto> getAll() {
        return super.getAllResources();
    }

    @Override
    public Mono<Void> putAll(List<Producto> productos) {
        return super.putAllResources(productos);
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
