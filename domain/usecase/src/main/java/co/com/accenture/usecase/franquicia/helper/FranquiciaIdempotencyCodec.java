package co.com.accenture.usecase.franquicia.helper;

import co.com.accenture.model.franquicia.Franquicia;
import co.com.accenture.usecase.idempotency.DelimitedIdempotencyPayloadCodec;
import co.com.accenture.usecase.idempotency.IdempotencyPayloadCodec;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public final class FranquiciaIdempotencyCodec implements IdempotencyPayloadCodec<Franquicia> {

    private static final DelimitedIdempotencyPayloadCodec<Franquicia> DELEGATE = DelimitedIdempotencyPayloadCodec.of(
        2,
            value -> Arrays.asList(
            value.getId() == null ? null : value.getId().toString(),
            value.getName()),
        values -> Franquicia.builder()
            .id(values.get(0) == null ? null : UUID.fromString(values.get(0)))
            .name(values.get(1))
            .build());

    public static final FranquiciaIdempotencyCodec INSTANCE = new FranquiciaIdempotencyCodec();

    private FranquiciaIdempotencyCodec() {
    }

    @Override
    public String serialize(Franquicia value) {
        return DELEGATE.serialize(value);
    }

    @Override
    public Mono<Franquicia> deserialize(String payload) {
        return DELEGATE.deserialize(payload);
    }
}
