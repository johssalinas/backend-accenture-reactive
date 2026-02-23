package co.com.accenture.usecase.sucursal.helper;

import co.com.accenture.model.sucursal.Sucursal;
import co.com.accenture.usecase.idempotency.DelimitedIdempotencyPayloadCodec;
import co.com.accenture.usecase.idempotency.IdempotencyPayloadCodec;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public final class SucursalIdempotencyCodec implements IdempotencyPayloadCodec<Sucursal> {

    private static final DelimitedIdempotencyPayloadCodec<Sucursal> DELEGATE = DelimitedIdempotencyPayloadCodec.of(
        3,
            value -> Arrays.asList(
            value.getId() == null ? null : value.getId().toString(),
            value.getName(),
            value.getFranquiciaId() == null ? null : value.getFranquiciaId().toString()),
        values -> Sucursal.builder()
            .id(values.get(0) == null ? null : UUID.fromString(values.get(0)))
            .name(values.get(1))
            .franquiciaId(values.get(2) == null ? null : UUID.fromString(values.get(2)))
            .build());

    public static final SucursalIdempotencyCodec INSTANCE = new SucursalIdempotencyCodec();

    private SucursalIdempotencyCodec() {
    }

    @Override
    public String serialize(Sucursal value) {
        return DELEGATE.serialize(value);
    }

    @Override
    public Mono<Sucursal> deserialize(String payload) {
        return DELEGATE.deserialize(payload);
    }
}
