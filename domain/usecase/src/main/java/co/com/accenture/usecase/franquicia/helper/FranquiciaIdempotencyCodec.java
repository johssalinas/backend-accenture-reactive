package co.com.accenture.usecase.franquicia.helper;

import co.com.accenture.model.franquicia.Franquicia;
import co.com.accenture.usecase.idempotency.IdempotencyPayloadCodec;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

public final class FranquiciaIdempotencyCodec implements IdempotencyPayloadCodec<Franquicia> {

    public static final FranquiciaIdempotencyCodec INSTANCE = new FranquiciaIdempotencyCodec();

    private FranquiciaIdempotencyCodec() {
    }

    @Override
    public String serialize(Franquicia value) {
        String encodedName = Base64.getUrlEncoder().encodeToString(value.getName().getBytes(StandardCharsets.UTF_8));
        return value.getId() + "|" + encodedName;
    }

    @Override
    public Mono<Franquicia> deserialize(String payload) {
        return Mono.fromSupplier(() -> {
            String[] parts = payload.split("\\|", 2);
            UUID id = UUID.fromString(parts[0]);
            String name = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            return Franquicia.builder().id(id).name(name).build();
        });
    }
}
